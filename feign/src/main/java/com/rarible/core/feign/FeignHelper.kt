package com.rarible.core.feign

import com.fasterxml.jackson.databind.ObjectMapper
import io.netty.channel.ChannelOption
import io.netty.channel.epoll.EpollChannelOption
import org.springframework.cloud.openfeign.support.SpringMvcContract
import org.springframework.http.MediaType
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.http.codec.ClientCodecConfigurer
import org.springframework.http.codec.json.Jackson2JsonDecoder
import org.springframework.http.codec.json.Jackson2JsonEncoder
import org.springframework.web.reactive.function.client.ExchangeStrategies
import org.springframework.web.reactive.function.client.WebClient
import reactivefeign.ReactiveContract
import reactivefeign.webclient.WebReactiveFeign
import reactor.netty.http.client.HttpClient

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

        val client = HttpClient.create().tcpConfiguration {
            it.option(ChannelOption.SO_KEEPALIVE, true)
                .option(EpollChannelOption.TCP_KEEPIDLE, 300)
                .option(EpollChannelOption.TCP_KEEPINTVL, 60)
                .option(EpollChannelOption.TCP_KEEPCNT, 8)
        }
        val connector = ReactorClientHttpConnector(client)
        return WebReactiveFeign
            .builder<T>(WebClient.builder().clientConnector(connector).exchangeStrategies(strategies))
            .contract(ReactiveContract(SpringMvcContract()))
            .target(clazz, baseUrl)
    }

    inline fun <reified T> createClient(mapper: ObjectMapper, baseUrl: String): T {
        return createClient(T::class.java, mapper, baseUrl)
    }
}