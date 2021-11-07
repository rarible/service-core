package com.rarible.core.apm.annotation

import com.rarible.core.apm.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.aopalliance.intercept.MethodInterceptor
import org.aopalliance.intercept.MethodInvocation
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
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

    private val checkCache = ConcurrentHashMap<MethodSignature, Boolean>()
    private val spanMethodCache = ConcurrentHashMap<MethodSignature, SpanMethod>()

    override fun invoke(invocation: MethodInvocation): Any? {
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

        abstract fun invoke(invocation: MethodInvocation): Any?

        companion object {
            fun createSpan(method: Method, spanInfo: SpanInfo): SpanMethod {
                return when (getMethodType(method)) {
                    MethodType.SUSPEND -> SuspendSpanMethod(spanInfo)
                    MethodType.MONO -> MonoSpanMethod(spanInfo)
                    MethodType.FLUX -> TODO()
                    MethodType.NORMAL -> TODO()
                }
            }

            fun createTransaction(method: Method, spanInfo: SpanInfo): SpanMethod {
                return when (getMethodType(method)) {
                    MethodType.SUSPEND -> SuspendTransactionMethod(spanInfo)
                    MethodType.MONO -> MonoTransactionMethod(spanInfo)
                    MethodType.FLUX -> TODO()
                    MethodType.NORMAL -> TODO()
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

            enum class MethodType {
                MONO,
                FLUX,
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

            override fun invoke(invocation: MethodInvocation): Any? {
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
            override fun invoke(invocation: MethodInvocation): Any? {
                return invocation.runCoroutine {
                    withSpan(info) {
                        invocation.proceedCoroutine()
                    }
                }
            }
        }

        data class SuspendTransactionMethod(
            override val info: SpanInfo
        ) : SpanMethod() {

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
}
