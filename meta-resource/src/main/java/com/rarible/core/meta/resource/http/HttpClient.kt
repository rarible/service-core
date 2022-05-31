package com.rarible.core.meta.resource.http

import com.rarible.core.meta.resource.http.builder.WebClientBuilder
import org.springframework.http.HttpHeaders
import org.springframework.web.reactive.function.client.WebClient
import java.time.Duration

abstract class HttpClient(
    builder: WebClientBuilder,
    requestTimeout: Long,
) {
    val client = builder.build()
    val timeout: Duration = Duration.ofMillis(requestTimeout)

    abstract fun getRequestHeadersUriSpec(): WebClient.RequestHeadersUriSpec<*>

    abstract fun match(url: String, useProxy: Boolean = false): Boolean
}

class DefaultHttpClient(
    builder: WebClientBuilder,
    requestTimeout: Long,
) : HttpClient(builder, requestTimeout) {

    override fun getRequestHeadersUriSpec() = client.get()

    override fun match(url: String, useProxy: Boolean) = true
}

class ProxyHttpClient(
    builder: WebClientBuilder,
    requestTimeout: Long
) : HttpClient(builder, requestTimeout) {

    override fun getRequestHeadersUriSpec() = client.get()

    override fun match(url: String, useProxy: Boolean) = useProxy
}

class OpenseaHttpClient(
    builder: WebClientBuilder,
    requestTimeout: Long,
    val openseaUrl: String,
    private val openseaApiKey: String,
    private val proxyUrl: String,
) : HttpClient(builder, requestTimeout) {

    override fun getRequestHeadersUriSpec(): WebClient.RequestHeadersUriSpec<*> {
        val proxyGet: WebClient.RequestHeadersUriSpec<*> = client.get()
        if (openseaApiKey.isNotBlank()) {
            proxyGet.header(X_API_KEY, openseaApiKey)
        }
        if (proxyUrl.isNotBlank()) {
            proxyGet.header(HttpHeaders.USER_AGENT, UserAgentGenerator.generateUserAgent())
        }
        return proxyGet
    }

    override fun match(url: String, useProxy: Boolean) = url.startsWith(openseaUrl)

    companion object {
        private const val X_API_KEY = "X-API-KEY"
    }
}
