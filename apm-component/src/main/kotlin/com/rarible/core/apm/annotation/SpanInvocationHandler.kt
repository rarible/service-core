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

        data class MonoTransactionMethod(
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

        data class SuspendSpanMethod(
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

        data class SuspendTransactionMethod(
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
                val defaultName = getMethodSignature(method, type)
                return when {
                    method.isAnnotationPresent(CaptureSpan::class.java) -> {
                        createSpan(method, defaultName)
                    }
                    method.isAnnotationPresent(CaptureTransaction::class.java) -> {
                        createTransaction(method, defaultName)
                    }
                    else -> NonSpanMethod
                }
            }

            fun createSpan(method: Method, defaultName: String): SpanMethod {
                val spanInfo = method.getAnnotation(CaptureSpan::class.java).let { annotation ->
                    SpanInfo(
                        name = annotation.value.ifNotBlank() ?: defaultName,
                        type = annotation.type.ifNotBlank(),
                        subType = annotation.subtype.ifNotBlank(),
                        action = annotation.action.ifNotBlank(),
                        labels = emptyList()
                    )
                }
                return when (getMethodType(method)) {
                    MethodType.SUSPEND -> AbstractSpanMethod.SuspendSpanMethod(spanInfo)
                    MethodType.MONO -> AbstractSpanMethod.MonoSpanMethod(spanInfo)
                    MethodType.FLUX -> TODO()
                    MethodType.NORMAL -> TODO()
                }
            }

            fun createTransaction(method: Method, defaultName: String): SpanMethod {
                val spanInfo = method.getAnnotation(CaptureTransaction::class.java).let { annotation ->
                    SpanInfo(
                        name = annotation.value.ifNotBlank() ?: defaultName
                    )
                }
                return when (getMethodType(method)) {
                    MethodType.SUSPEND -> AbstractSpanMethod.SuspendTransactionMethod(spanInfo)
                    MethodType.MONO -> AbstractSpanMethod.MonoTransactionMethod(spanInfo)
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

            private fun String.ifNotBlank(): String? = ifBlank { null }

            private fun getMethodSignature(method: Method, type: Class<*>): MethodSignature = "${type.name}#${method.name}"
        }
    }
}
