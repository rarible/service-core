package com.rarible.core.apm.annotation

import co.elastic.apm.api.ElasticApm
import co.elastic.apm.api.Span
import com.rarible.core.apm.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.reactor.ReactorContext
import org.aopalliance.intercept.MethodInterceptor
import org.aopalliance.intercept.MethodInvocation
import reactor.core.publisher.Mono
import reactor.util.context.Context
import java.lang.reflect.Method
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.Continuation
import kotlin.coroutines.CoroutineContext

typealias MethodSignature = String

class MonoSpanInvocationHandler(
    private val type: Class<*>
) : MethodInterceptor {

    private val checkCache = ConcurrentHashMap<MethodSignature, Boolean>()
    private val spanMethodCache = ConcurrentHashMap<MethodSignature, SpanMethod>()

    override fun invoke(invocation: MethodInvocation): Any {
        val method = invocation.method
        val signature = getMethodSignature(method)

        return when (checkCache[signature]) {
            false -> {
                invocation.proceed()
            }
            true -> {
                val spanMethod = spanMethodCache[signature] ?: error("Cache must exist for signature $signature")
                spanMethod.invoke(invocation)
            }
            null -> {
                val original = type.getMethod(method.name, *method.parameterTypes)
                val spanMethod = extractSpanMethod(original, signature)

                if (spanMethod != null) {
                    checkCache[signature] = true
                    spanMethodCache[signature] = spanMethod

                    spanMethod.invoke(invocation)
                } else {
                    checkCache[signature] = false
                    invocation.proceed()
                }
            }
        }
    }

    private fun extractSpanMethod(method: Method, defaultName: String): SpanMethod? {
        return when {
            method.isAnnotationPresent(CaptureSpan::class.java) -> {
                val info = method.getAnnotation(CaptureSpan::class.java).let { annotation ->
                    SpanInfo(
                        name = annotation.value.ifNotBlack() ?: defaultName,
                        type = annotation.type.ifNotBlack(),
                        subType = annotation.subtype.ifNotBlack(),
                        action = annotation.action.ifNotBlack(),
                        labels = emptyList()
                    )
                }
                SpanMethod.createSpan(method, info)
            }
            method.isAnnotationPresent(CaptureTransaction::class.java) -> {
                val info = method.getAnnotation(CaptureTransaction::class.java).let { annotation ->
                    SpanInfo(
                        name = annotation.value.ifNotBlack() ?: defaultName
                    )
                }
                SpanMethod.createTransaction(method, info)
            }
            else -> null
        }
    }

    private fun getMethodSignature(method: Method): MethodSignature = "${type.name}#${method.name}"

    private fun String.ifNotBlack(): String? = ifBlank { null }

    private sealed class SpanMethod {
        protected abstract val info: SpanInfo

        abstract fun invoke(invocation: MethodInvocation): Any

        companion object {
            fun createSpan(method: Method, spanInfo: SpanInfo): SpanMethod {
                return when (getMethodType(method)) {
                    MethodType.SUSPEND -> SuspendSpanMethod(spanInfo)
                    MethodType.MONO -> MonoSpanMethod(spanInfo)
                    MethodType.NORMAL -> TODO()
                }
            }

            fun createTransaction(method: Method, spanInfo: SpanInfo): SpanMethod {
                return when (getMethodType(method)) {
                    MethodType.SUSPEND -> SuspendTransactionMethod(spanInfo)
                    MethodType.MONO -> MonoTransactionMethod(spanInfo)
                    MethodType.NORMAL -> TODO()
                }
            }

            private fun getMethodType(method: Method): MethodType {
                return when {
                    method.returnType == Mono::class.java -> MethodType.MONO
                    method.parameterCount > 0 && method.parameterTypes.last() == Continuation::class.java -> MethodType.SUSPEND
                    else -> MethodType.NORMAL
                }
            }

            enum class MethodType {
                MONO,
                SUSPEND,
                NORMAL
            }
        }

        data class MonoSpanMethod(
            override val info: SpanInfo
        ) : SpanMethod() {

            override fun invoke(invocation: MethodInvocation): Any {
                val result = invocation.proceed() as Mono<*>
                return result.withSpan(
                    name = info.name,
                    type = info.type,
                    subType = info.subType,
                    action = info.action,
                    labels = info.labels
                )
            }
        }

        data class MonoTransactionMethod(
            override val info: SpanInfo
        ) : SpanMethod() {

            override fun invoke(invocation: MethodInvocation): Any {
                val result = invocation.proceed() as Mono<*>
                return result.withTransaction(
                    name = info.name,
                    labels = info.labels
                )
            }
        }

        data class SuspendSpanMethod(
           override val info: SpanInfo
        ) : SpanMethod() {

            @ExperimentalCoroutinesApi
            override fun invoke(invocation: MethodInvocation): Any {
                val arguments = invocation.arguments
                val continuation = arguments.last() as Continuation<*>

                val reactorContext = continuation.context[ReactorContext.Key]?.context
                val apmContext = reactorContext?.get<ApmContext>(ApmContext.Key)

                if (apmContext != null) {
                    val span = apmContext.span.startSpan(info.type, info.subType, info.action)
                    span.setName(info.name)

                    val spanWrappedContinuation = SpanWrappedContinuation(continuation, span, reactorContext)
                    arguments[arguments.size - 1] = spanWrappedContinuation
                }
                return invocation.proceed()
            }
        }

        data class SuspendTransactionMethod(
            override val info: SpanInfo
        ) : SpanMethod() {

            @ExperimentalCoroutinesApi
            override fun invoke(invocation: MethodInvocation): Any {
                val arguments = invocation.arguments
                val continuation = arguments.last() as Continuation<*>

                val reactorContext = continuation.context[ReactorContext.Key]?.context ?: Context.empty()

                val transaction = ElasticApm.startTransaction()
                transaction.setName(info.name)

                val spanWrappedContinuation = SpanWrappedContinuation(continuation, transaction, reactorContext)
                arguments[arguments.size - 1] = spanWrappedContinuation

                return invocation.proceed()
            }
        }

        @ExperimentalCoroutinesApi
        class SpanWrappedContinuation<T>(
            private val continuation: Continuation<T>,
            private val span: Span,
            reactorContext: Context
        ) : Continuation<T> {
            private val contextWithSpan = putApmContext(span, reactorContext, continuation)

            override val context: CoroutineContext
                get() = contextWithSpan

            override fun resumeWith(result: Result<T>) {
                when {
                    result.isSuccess -> span.end()
                    result.isFailure -> span.captureException(result.exceptionOrNull())
                }
                continuation.resumeWith(result)
            }

            private fun putApmContext(span: Span, reactorContext: Context, continuation: Continuation<T>): CoroutineContext {
                return continuation.context + ReactorContext(reactorContext.put(ApmContext.Key, ApmContext(span)))
            }
        }
    }
}
