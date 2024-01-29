package com.rarible.core.meta.resource.http.builder

import com.rarible.core.meta.resource.http.builder.DefaultWebClientBuilder.Companion.DEFAULT_TIMEOUT
import io.netty.handler.ssl.SslContextBuilder
import io.netty.handler.ssl.util.InsecureTrustManagerFactory
import io.netty.handler.timeout.ReadTimeoutHandler
import org.springframework.http.HttpHeaders
import org.springframework.http.client.reactive.ClientHttpConnector
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.web.reactive.function.client.ExchangeStrategies
import org.springframework.web.reactive.function.client.WebClient
import reactor.netty.http.client.HttpClient
import reactor.netty.resources.ConnectionProvider
import reactor.netty.transport.ProxyProvider
import java.net.URI
import java.util.concurrent.TimeUnit

class ProxyWebClientBuilder(
    private val readTimeout: Int,
    private val connectTimeout: Int,
    private val proxyUrl: String,
    private val followRedirect: Boolean,
    private val defaultHeaders: HttpHeaders = HttpHeaders(),
    private val insecure: Boolean = false,
) : WebClientBuilder {

    override fun build(): WebClient =
        WebClient.builder()
            .clientConnector(
                createConnector(
                    connectTimeoutMs = connectTimeout,
                    readTimeoutMs = readTimeout,
                    proxyUrl = proxyUrl,
                    followRedirect = followRedirect
                )
            )
            .exchangeStrategies(
                ExchangeStrategies.builder()
                    .codecs { it.defaultCodecs().maxInMemorySize(262144 * 5) }
                    .build())
            .defaultHeaders { headers -> headers.addAll(defaultHeaders) }
            .build()

    private fun createConnector(
        connectTimeoutMs: Int,
        readTimeoutMs: Int,
        proxyUrl: String,
        @Suppress("SameParameterValue") followRedirect: Boolean
    ): ClientHttpConnector {
        val provider = ConnectionProvider.builder("protocol-default-open_sea-connection-provider")
            .maxConnections(200)
            .pendingAcquireMaxCount(-1)
            .maxIdleTime(DEFAULT_TIMEOUT)
            .maxLifeTime(DEFAULT_TIMEOUT)
            .lifo()
            .build()

        val tcpClient = reactor.netty.tcp.TcpClient.create(provider)
            .option(io.netty.channel.ChannelOption.CONNECT_TIMEOUT_MILLIS, connectTimeoutMs)
            .doOnConnected {
                it.addHandlerLast(ReadTimeoutHandler(readTimeoutMs.toLong(), TimeUnit.MILLISECONDS))
            }

        val sslContext = SslContextBuilder
            .forClient()
            .apply {
                if (insecure) {
                    trustManager(InsecureTrustManagerFactory.INSTANCE)
                }
            }
            .build()

        if (proxyUrl.isNotBlank()) {
            val finalTcpClient = tcpClient.proxy {
                val uri = URI.create(proxyUrl)
                val user = uri.userInfo.split(":")
                it.type(ProxyProvider.Proxy.HTTP)
                    .host(uri.host)
                    .username(user[0])
                    .password { user[1] }
                    .port(uri.port)
            }
            return ReactorClientHttpConnector(
                HttpClient.from(finalTcpClient).followRedirect(followRedirect)
                    .secure { sslProvider -> sslProvider.sslContext(sslContext) }
            )
        }

        return ReactorClientHttpConnector(
            HttpClient.from(tcpClient).followRedirect(followRedirect)
                .secure { sslProvider -> sslProvider.sslContext(sslContext) }
        )
    }
}
