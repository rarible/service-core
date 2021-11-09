package com.rarible.core.apm.annotation

import com.rarible.core.apm.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.aopalliance.intercept.MethodInterceptor
import org.aopalliance.intercept.MethodInvocation
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.lang.reflect.AnnotatedElement
import java.lang.reflect.Method
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.Continuation
import kotlin.coroutines.intrinsics.startCoroutineUninterceptedOrReturn
import kotlin.coroutines.intrinsics.suspendCoroutineUninterceptedOrReturn

typealias MethodSignature = String

@Suppress("UNCHECKED_CAST")
class MonoSpanInvocationHandler(
    private val type: Class<*>
) : MethodInterceptor {

    private val spanMethodCache = ConcurrentHashMap<Method, SpanMethod>()

    override fun invoke(invocation: MethodInvocation): Any? {
        return spanMethodCache
            .computeIfAbsent(invocation.method) { method -> SpanMethodFactory.create(method, type) }
            .invoke(invocation)
    }

    private interface SpanMethod {
        fun invoke(invocation: MethodInvocation): Any?
    }

    object NonSpanMethod : SpanMethod {
        override fun invoke(invocation: MethodInvocation): Any? {
            return invocation.proceed()
        }
    }

    private sealed class AbstractSpanMethod : SpanMethod {
        protected abstract val info: SpanInfo

        class MonoSpanMethod(
            override val info: SpanInfo
        ) : AbstractSpanMethod() {

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

        class MonoTransactionMethod(
            override val info: SpanInfo
        ) : AbstractSpanMethod() {

            override fun invoke(invocation: MethodInvocation): Any {
                val result = invocation.proceed() as Mono<*>
                return result.withTransaction(
                    name = info.name,
                    labels = info.labels
                )
            }
        }

        class SuspendSpanMethod(
           override val info: SpanInfo
        ) : AbstractSpanMethod() {

            @ExperimentalCoroutinesApi
            override fun invoke(invocation: MethodInvocation): Any? {
                return invocation.runCoroutine {
                    withSpan(info) {
                        invocation.proceedCoroutine()
                    }
                }
            }
        }

        class SuspendTransactionMethod(
            override val info: SpanInfo
        ) : AbstractSpanMethod() {

            @ExperimentalCoroutinesApi
            override fun invoke(invocation: MethodInvocation): Any? {
                return invocation.runCoroutine {
                    withTransaction(info.name) {
                        invocation.proceedCoroutine()
                    }
                }
            }
        }

        fun MethodInvocation.runCoroutine(block: suspend () -> Any?): Any? {
            return block.startCoroutineUninterceptedOrReturn(this.coroutineContinuation)
        }

        val MethodInvocation.coroutineContinuation: Continuation<Any?>
            get() = this.arguments.last() as Continuation<Any?>

        suspend fun MethodInvocation.proceedCoroutine(): Any? {
            return suspendCoroutineUninterceptedOrReturn { continuation ->
                this.arguments[this.arguments.size - 1] = continuation
                this.proceed()
            }
        }
    }

    private class SpanMethodFactory {
        companion object {
            fun create(method: Method, type: Class<*>): SpanMethod {
                return when {
                    hasCaptureSpanAnnotations(method) || hasCaptureSpanAnnotations(type) -> {
                        createSpan(method, getSpanInfo(method, type))
                    }
                    hasCaptureTransactionAnnotations(method) || hasCaptureTransactionAnnotations(type) -> {
                        createTransaction(method, getSpanInfo(method, type))
                    }
                    else -> NonSpanMethod
                }
            }

            fun createSpan(method: Method, spanInfo: SpanInfo): SpanMethod {
                return when (getMethodType(method)) {
                    MethodType.SUSPEND -> AbstractSpanMethod.SuspendSpanMethod(spanInfo)
                    MethodType.MONO -> AbstractSpanMethod.MonoSpanMethod(spanInfo)
                    MethodType.FLUX -> NonSpanMethod // TODO Need implement
                    MethodType.NORMAL -> NonSpanMethod // TODO Need implement
                }
            }

            fun createTransaction(method: Method, spanInfo: SpanInfo): SpanMethod {
                return when (getMethodType(method)) {
                    MethodType.SUSPEND -> AbstractSpanMethod.SuspendTransactionMethod(spanInfo)
                    MethodType.MONO -> AbstractSpanMethod.MonoTransactionMethod(spanInfo)
                    MethodType.FLUX -> NonSpanMethod // TODO Need implement
                    MethodType.NORMAL -> NonSpanMethod // TODO Need implement
                }
            }

            private fun getMethodType(method: Method): MethodType {
                return when {
                    method.returnType == Mono::class.java -> MethodType.MONO
                    method.returnType == Flux::class.java -> MethodType.FLUX
                    method.parameterCount > 0 && method.parameterTypes.last() == Continuation::class.java -> MethodType.SUSPEND
                    else -> MethodType.NORMAL
                }
            }

            private fun getSpanInfo(method: Method, type: Class<*>): SpanInfo {
                val methodSpanInfo = fetchSpanInfo(method)
                val typeSpanInfo = fetchSpanInfo(type)
                return SpanInfo(
                    name = methodSpanInfo.name.ifNotBlank() ?: typeSpanInfo.name.ifNotBlank() ?: getMethodSignature(method, type),
                    type = methodSpanInfo.type?.ifNotBlank() ?: typeSpanInfo.type?.ifNotBlank(),
                    subType = methodSpanInfo.subType?.ifNotBlank() ?: typeSpanInfo.subType?.ifNotBlank(),
                    action = methodSpanInfo.action?.ifNotBlank() ?: typeSpanInfo.action?.ifNotBlank(),
                    labels = methodSpanInfo.labels + typeSpanInfo.labels
                )
            }

            private fun fetchSpanInfo(element: AnnotatedElement): SpanInfo {
                return when {
                    hasCaptureSpanAnnotations(element) -> {
                        element.getAnnotation(CaptureSpan::class.java).let { annotation ->
                            SpanInfo(
                                name = annotation.value,
                                type = annotation.type.ifNotBlank(),
                                subType = annotation.subtype.ifNotBlank(),
                                action = annotation.action.ifNotBlank()
                            )
                        }
                    }
                    hasCaptureTransactionAnnotations(element) -> {
                        element.getAnnotation(CaptureTransaction::class.java).let { annotation ->
                            SpanInfo(
                                name = annotation.value
                            )
                        }
                    }
                    else -> {
                        EMPTY_SPAN_INFO
                    }
                }
            }

            enum class MethodType {
                MONO,
                FLUX,
                SUSPEND,
                NORMAL
            }

            val hasCaptureSpanAnnotations: (AnnotatedElement) -> Boolean = { element ->
                element.isAnnotationPresent(CaptureSpan::class.java)
            }

            val hasCaptureTransactionAnnotations: (AnnotatedElement) -> Boolean = { element ->
                element.isAnnotationPresent(CaptureTransaction::class.java)
            }

            private fun String.ifNotBlank(): String? = ifBlank { null }

            private fun getMethodSignature(method: Method, type: Class<*>): MethodSignature = "${type.name}#${method.name}"

            private val EMPTY_SPAN_INFO = SpanInfo(
                name = "",
                type = null,
                subType = null,
                action = null,
                labels = emptyList()
            )
        }
    }
}
