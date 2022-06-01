package com.rarible.core.meta.resource.http

import com.rarible.core.meta.resource.MetaLogger.logMetaLoading
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseEntity
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import org.springframework.web.reactive.function.client.bodyToMono
import java.time.Duration

/**
 * Client responsible for making HTTP requests to external APIs.
 * Handles OpenSea's API separately (by using a dedicated HTTP proxy).
 */

open class ExternalHttpClient(
    private val defaultClient: HttpClient,
    private val proxyClient: HttpClient,
    private val customClients: List<HttpClient>
) {

    suspend fun getEntity(url: String, bodyClass: Class<*>, useProxy: Boolean = false, id: String): ResponseEntity<*>? {
        val (responseSpec, timeout) = getResponseSpec(url, useProxy, id) ?: return null
        return try {
            responseSpec
                ?.toEntity(bodyClass)
                ?.timeout(timeout)
                ?.awaitFirstOrNull()
        } catch (e: Exception) {
            logMetaLoading(id, "failed to get properties by URI $url: ${e.message} ${getResponse(e)}", warn = true)
            null
        }
    }

    suspend fun getHeaders(url: String, useProxy: Boolean = false, id: String): HttpHeaders? {
        val (responseSpec, timeout) = getResponseSpec(url, useProxy, id) ?: return null
        return try {
            responseSpec
                ?.toBodilessEntity()
                ?.timeout(timeout)
                ?.awaitFirstOrNull()
                ?.headers
        } catch (e: Exception) {
            logMetaLoading(id, "failed to get properties by URI $url: ${e.message} ${getResponse(e)}", warn = true)
            null
        }
    }

    suspend fun getBody(url: String, useProxy: Boolean = false, id: String): String? {
        val (responseSpec, timeout) = getResponseSpec(url, useProxy, id) ?: return null
        return try {
            responseSpec
                ?.bodyToMono<String>()
                ?.timeout(timeout)
                ?.awaitFirstOrNull()
        } catch (e: Exception) {
            logMetaLoading(id, "failed to get properties by URI $url: ${e.message} ${getResponse(e)}", warn = true)
            null
        }
    }

    suspend fun getEtag(url: String, useProxy: Boolean = false, id: String): String? =
        getHeaders(url = url, useProxy = useProxy, id = id)
            ?.getFirst("etag")
            ?.replace("\"", "")

    private fun getResponseSpec(
        url: String,
        useProxy: Boolean = false,
        id: String
    ): Pair<WebClient.ResponseSpec?, Duration>? {
        if (url.isBlank()) return null

        return try {
            val client = routeClient(url, useProxy)
            val requestHeadersUriSpec = client.getRequestHeadersUriSpec()

            // May throw "invalid URL" exception.
            requestHeadersUriSpec.uri(url)
            Pair(requestHeadersUriSpec.retrieve(), client.timeout)
        } catch (e: Exception) {
            logMetaLoading(id, "failed to parse URI: $url: ${e.message}", warn = true)
            null
        }
    }

    private fun routeClient(url: String, useProxy: Boolean = false): HttpClient {
        for (client in customClients) {
            if (client.match(url, useProxy)) return client
        }
        return when {
            proxyClient.match(url = url, useProxy = useProxy) -> proxyClient
            else -> defaultClient
        }
    }

    private fun getResponse(e: Exception): String =
        if (e is WebClientResponseException) {
            " response: ${e.rawStatusCode}: ${e.statusText}"
        } else {
            ""
        }
}



