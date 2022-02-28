package com.rarible.core.content.meta.loader

import io.netty.channel.ChannelOption
import io.netty.handler.timeout.ReadTimeoutHandler
import io.netty.handler.timeout.WriteTimeoutHandler
import io.netty.resolver.DefaultAddressResolverGroup
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.springframework.core.io.buffer.DataBufferUtils
import org.springframework.http.HttpHeaders
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.web.reactive.function.BodyExtractors
import org.springframework.web.reactive.function.client.ExchangeStrategies
import org.springframework.web.reactive.function.client.WebClient
import reactor.netty.http.client.HttpClient
import java.net.URL
import java.time.Duration
import java.util.concurrent.TimeUnit

/**
 * Spring WebClient implementation of [ContentReceiver].
 */
// TODO: most probably, we have a memory leak here (need to close byte buffers).
class WebClientContentReceiver(
    private val maxBodySize: Int,
    private val responseTimeout: Int
) : ContentReceiver {

    private val httpClient = HttpClient.create()
        .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, responseTimeout)
        .responseTimeout(Duration.ofMillis(responseTimeout.toLong()))
        .doOnConnected { connection ->
            connection.addHandlerLast(ReadTimeoutHandler(responseTimeout.toLong(), TimeUnit.MILLISECONDS))
            connection.addHandlerLast(WriteTimeoutHandler(responseTimeout.toLong(), TimeUnit.MILLISECONDS))
        }
        .followRedirect(true)
        .resolver(DefaultAddressResolverGroup.INSTANCE)

    private val webClient = WebClient.builder()
        .exchangeStrategies(
            ExchangeStrategies.builder()
                .codecs { it.defaultCodecs().maxInMemorySize(maxBodySize) }
                .build()
        )
        .clientConnector(ReactorClientHttpConnector(httpClient))
        .build()

    override suspend fun receiveBytes(
        url: URL,
        maxBytes: Int
    ): ContentBytes =
        webClient
            .get()
            .uri(url.toURI())
            .header(HttpHeaders.CONTENT_RANGE)
            .exchangeToMono { clientResponse ->
                val mediaType = clientResponse.headers().contentType().orElse(null)
                val contentLength = clientResponse.headers().contentLength().orElse(-1L).takeIf { it != -1L }
                val fluxBuffers = clientResponse.body(BodyExtractors.toDataBuffers())
                val bytes = DataBufferUtils.takeUntilByteCount(fluxBuffers, maxBytes.toLong())
                DataBufferUtils.join(bytes, maxBytes)
                    .map {
                        val byteArray = ByteArray(it.readableByteCount())
                        it.read(byteArray)
                        ContentBytes(byteArray, mediaType?.type, contentLength)
                    }
            }.awaitFirst()

    override fun close() = Unit
}
