package com.rarible.core.feign

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.cloud.openfeign.support.SpringMvcContract
import org.springframework.http.MediaType
import org.springframework.http.codec.ClientCodecConfigurer
import org.springframework.http.codec.json.Jackson2JsonDecoder
import org.springframework.http.codec.json.Jackson2JsonEncoder
import org.springframework.web.reactive.function.client.ExchangeStrategies
import org.springframework.web.reactive.function.client.WebClient
import reactivefeign.ReactiveContract
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
            .target(clazz, baseUrl)
    }

    inline fun <reified T> createClient(mapper: ObjectMapper, baseUrl: String): T {
        return createClient(T::class.java, mapper, baseUrl)
    }
}