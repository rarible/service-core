package com.rarible.core.apm.annotation

import com.rarible.core.apm.*
import reactor.core.publisher.Mono
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.util.concurrent.ConcurrentHashMap

typealias MethodSignature = String

class MonoSpanInvocationHandler(
    private val type: Class<*>,
    private val bean: Any
) : InvocationHandler {
    private val checkCache = ConcurrentHashMap<MethodSignature, Boolean>()
    private val infoCache = ConcurrentHashMap<MethodSignature, SpanCache>()

    override fun invoke(proxy: Any, method: Method, args: Array<out Any>): Any {
        val signature = getMethodSignature(method)

        return when (checkCache[signature]) {
            false -> {
                method.invoke(method, args)
            }
            true -> {
                val cache = infoCache[signature] ?: error("Cache must exist for signature $signature")
                invoke(cache, method, args)
            }
            null -> {
                val original = type.getMethod(method.name, *method.parameterTypes)

                val validReturnType = method.returnType is Mono<*>
                val cache = extractCacheInfo(signature, original)

                if (validReturnType && cache != null) {
                    checkCache[signature] = true
                    infoCache[signature] = cache

                    invoke(cache, method, args)
                } else {
                    checkCache[signature] = false
                    method.invoke(method, args)
                }
            }
        }
    }

    private fun extractCacheInfo(methodSignature: MethodSignature, method: Method): SpanCache? {
        return when {
            method.isAnnotationPresent(CaptureSpan::class.java) -> {
                val spanAnnotation = method.getAnnotation(CaptureSpan::class.java)
                SpanCache(
                    type = SpanCache.Type.SPAN,
                    info = SpanInfo(
                        name = spanAnnotation.value.ifNotBlack() ?: methodSignature,
                        type = spanAnnotation.type.ifNotBlack(),
                        subType = spanAnnotation.subtype.ifNotBlack(),
                        action = spanAnnotation.action.ifNotBlack(),
                        labels = emptyList()
                    )
                )
            }
            method.isAnnotationPresent(CaptureTransaction::class.java) -> {
                val spanAnnotation = method.getAnnotation(CaptureTransaction::class.java)
                SpanCache(
                    type = SpanCache.Type.TRANSACTION,
                    info = SpanInfo(
                        name = spanAnnotation.value.ifNotBlack() ?: methodSignature
                    )
                )
            }
            else -> null
        }
    }

    private fun getMethodSignature(method: Method): MethodSignature = "${type.name}#${method.name}"

    private fun invoke(spanCache: SpanCache, method: Method, args: Array<out Any>): Any {
        val result = method.invoke(bean, args) as Mono<*>
        val spanInfo = spanCache.info

        return when (spanCache.type) {
            SpanCache.Type.SPAN ->
                result.withSpan(
                    name = spanInfo.name,
                    type = spanInfo.type,
                    subType = spanInfo.subType,
                    action = spanInfo.action,
                    labels = spanInfo.labels
                )

            SpanCache.Type.TRANSACTION ->
                result.withTransaction(
                    name = spanInfo.name,
                    labels = spanInfo.labels
                )
        }
    }

    private fun String.ifNotBlack(): String? = ifBlank { null }

    private data class SpanCache(
        val info: SpanInfo,
        val type: Type
    ) {
        enum class Type {
            TRANSACTION,
            SPAN
        }
    }
}
