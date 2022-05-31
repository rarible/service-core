package com.rarible.core.meta.resource.http

import org.springframework.http.HttpHeaders
import org.springframework.web.reactive.function.client.WebClient

/**
 * Client responsible for making HTTP requests to external APIs.
 * Handles OpenSea's API separately (by using a dedicated HTTP proxy).
 */

open class ExternalHttpClient(
    val openseaUrl: String,
    private val openseaApiKey: String,
    private val proxyUrl: String,
    proxyWebClientBuilder: ProxyWebClientBuilder,
    defaultWebClientBuilder: DefaultWebClientBuilder
) {

    protected open val defaultClient: WebClient = defaultWebClientBuilder.build()

    protected open val proxyClient: WebClient = proxyWebClientBuilder.build()

    fun isOpensea(url: String) = url.startsWith(openseaUrl)

    fun get(url: String, useProxy: Boolean = false): WebClient.ResponseSpec {
        val requestHeadersUriSpec = when {
            isOpensea(url) -> proxyClient.getOpensea()
            useProxy -> proxyClient.get()
            else -> defaultClient.get()
        }

        // May throw "invalid URL" exception.
        requestHeadersUriSpec.uri(url)
        return requestHeadersUriSpec.retrieve()
    }

    private fun WebClient.getOpensea(): WebClient.RequestHeadersUriSpec<*> {
        val proxyGet = this.get()
        if (openseaApiKey.isNotBlank()) {
            proxyGet.header(X_API_KEY, openseaApiKey)
        }
        if (proxyUrl.isNotBlank()) {
            proxyGet.header(HttpHeaders.USER_AGENT, UserAgentGenerator.generateUserAgent())
        }
        return proxyGet
    }

    companion object {
        private const val X_API_KEY = "X-API-KEY"
    }
}




