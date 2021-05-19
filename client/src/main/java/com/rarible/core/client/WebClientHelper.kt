package com.rarible.core.client

import com.fasterxml.jackson.databind.ObjectMapper
import com.google.common.base.CaseFormat
import com.rarible.core.logging.LoggerContext.MDC_MAP
import org.springframework.http.MediaType
import org.springframework.http.client.reactive.ClientHttpConnector
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.http.codec.ClientCodecConfigurer
import org.springframework.http.codec.json.Jackson2JsonDecoder
import org.springframework.http.codec.json.Jackson2JsonEncoder
import org.springframework.web.reactive.function.client.ExchangeStrategies
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import java.util.concurrent.TimeUnit
import java.util.function.Consumer

object WebClientHelper {
    @JvmStatic
    val LOG_HEADERS: Mono<Map<String, String>> = MDC_MAP
        .map { it.toHeadersMap() }

    @JvmStatic
    fun createConnector(connectTimeoutMs: Int, readTimeoutMs: Int): ClientHttpConnector? {
        return createConnector(connectTimeoutMs, readTimeoutMs, false)
    }

    @JvmStatic
    fun createConnector(connectTimeoutMs: Int, readTimeoutMs: Int, followRedirect: Boolean): ClientHttpConnector {
        val tcpClient: reactor.netty.tcp.TcpClient = reactor.netty.tcp.TcpClient.create()
            .option(io.netty.channel.ChannelOption.CONNECT_TIMEOUT_MILLIS, connectTimeoutMs)
            .doOnConnected { conn: reactor.netty.Connection ->
                conn.addHandlerLast(
                    io.netty.handler.timeout.ReadTimeoutHandler(readTimeoutMs.toLong(), TimeUnit.MILLISECONDS)
                )
            }
        return ReactorClientHttpConnector(
            reactor.netty.http.client.HttpClient.from(tcpClient).followRedirect(followRedirect)
        )
    }

    @JvmStatic
    fun createExchangeStrategies(objectMapper: ObjectMapper): ExchangeStrategies? {
        return createExchangeStrategies(objectMapper, 1048576)
    }

    @JvmStatic
    fun createExchangeStrategies(objectMapper: ObjectMapper, maxInMemorySizeBytes: Int): ExchangeStrategies? {
        return ExchangeStrategies.builder()
            .codecs { conf: ClientCodecConfigurer ->
                conf.defaultCodecs().maxInMemorySize(maxInMemorySizeBytes)
                conf.defaultCodecs()
                    .jackson2JsonDecoder(Jackson2JsonDecoder(objectMapper, MediaType.APPLICATION_JSON))
                conf.defaultCodecs()
                    .jackson2JsonEncoder(Jackson2JsonEncoder(objectMapper, MediaType.APPLICATION_JSON))
            }
            .build()
    }

    @JvmStatic
    fun preprocess(requestBuilder: WebClient.RequestBodySpec): Mono<WebClient.RequestBodySpec> {
        return LOG_HEADERS
            .map { preprocess(requestBuilder, it) }
    }

    private fun preprocess(requestBuilder: WebClient.RequestBodySpec, headers: Map<String, String>) =
        headers.entries.fold(requestBuilder) { rb, entry ->
            rb.header(entry.key, entry.value)
        }
}

fun Map<String, String>.toHeadersMap() =
    this.map { entry -> "x-log-${CONVERTER.convert(entry.key)}" to entry.value }
        .toMap()

private val CONVERTER = CaseFormat.LOWER_CAMEL.converterTo(CaseFormat.LOWER_HYPHEN)
