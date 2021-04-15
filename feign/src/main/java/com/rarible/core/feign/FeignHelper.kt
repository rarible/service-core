package com.rarible.core.feign

import com.fasterxml.jackson.databind.ObjectMapper
import com.google.common.base.CaseFormat
import com.rarible.core.logging.MDC_CONTEXT
import kotlinx.coroutines.slf4j.MDCContext
import org.springframework.cloud.openfeign.support.SpringMvcContract
import org.springframework.http.MediaType
import org.springframework.http.codec.ClientCodecConfigurer
import org.springframework.http.codec.json.Jackson2JsonDecoder
import org.springframework.http.codec.json.Jackson2JsonEncoder
import org.springframework.web.reactive.function.client.ExchangeStrategies
import org.springframework.web.reactive.function.client.WebClient
import reactivefeign.ReactiveContract
import reactivefeign.ReactiveFeignBuilder
import reactivefeign.client.ReactiveHttpRequest
import reactivefeign.utils.MultiValueMapUtils
import reactivefeign.webclient.WebReactiveFeign

object FeignHelper {
    fun <T> createClient(clazz: Class<T>, mapper: ObjectMapper, baseUrl: String): T {
        val strategies = ExchangeStrategies
            .builder()
            .codecs { clientDefaultCodecsConfigurer: ClientCodecConfigurer ->
                clientDefaultCodecsConfigurer.defaultCodecs().jackson2JsonEncoder(
                    Jackson2JsonEncoder(mapper, MediaType.APPLICATION_JSON)
                )
                clientDefaultCodecsConfigurer.defaultCodecs().jackson2JsonDecoder(
                    Jackson2JsonDecoder(mapper, MediaType.APPLICATION_JSON)
                )
            }.build()

        return WebReactiveFeign
            .builder<T>(WebClient.builder().exchangeStrategies(strategies))
            .contract(ReactiveContract(SpringMvcContract()))
            .addRequestInterceptor { req ->
                MDC_CONTEXT.map {
                    if (it.isPresent) {
                        addHeaders(req, it.get())
                    } else {
                        req
                    }
                }
            }
            .target(clazz, baseUrl)
    }

    private fun addHeaders(req: ReactiveHttpRequest, mdc: MDCContext): ReactiveHttpRequest {
        mdc.contextMap
            ?.toHeadersMap()
            ?.forEach { key, value ->
                MultiValueMapUtils.add(req.headers() as Map<String, MutableCollection<String>>?, key, value)
            }
        return req
    }

    inline fun <reified T> createClient(mapper: ObjectMapper, baseUrl: String): T {
        return createClient(T::class.java, mapper, baseUrl)
    }
}

fun Map<String, String>.toHeadersMap() =
    this.map { entry -> "x-log-${CONVERTER.convert(entry.key)}" to entry.value }
        .toMap()

private val CONVERTER = CaseFormat.LOWER_CAMEL.converterTo(CaseFormat.LOWER_HYPHEN)
