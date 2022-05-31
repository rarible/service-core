package com.rarible.core.meta.resource.http

import com.rarible.core.meta.resource.MetaLogger.logMetaLoading
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.springframework.http.HttpHeaders
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import java.time.Duration

class PropertiesHttpLoader(
    private val externalHttpClient: ExternalHttpClient,
    defaultRequestTimeout: Long,
    openseaRequestTimeout: Long
) {

    private val apiDefaultTimeout = Duration.ofMillis(defaultRequestTimeout)
    private val apiOpenseaTimeout = Duration.ofMillis(openseaRequestTimeout)

    suspend fun getEntity(url: String, bodyClass: Class<*>, useProxy: Boolean = false, id: String) =
        try {
            getResponseSpec(url, useProxy, id)?.toEntity(bodyClass)
                ?.timeout(getRequestTimeout(url))
                ?.awaitFirstOrNull()
        } catch (e: Exception) {
            logMetaLoading(id, "failed to get properties by URI $url: ${e.message}", warn = true)
            null
        }

    suspend fun getHeaders(url: String, useProxy: Boolean = false, id: String): HttpHeaders? =
        try {
            getResponseSpec(url, useProxy, id)
                ?.toBodilessEntity()
                ?.timeout(getRequestTimeout(url))
                ?.awaitFirstOrNull()
                ?.headers
        } catch (e: Exception) {
            logMetaLoading(id, "failed to get properties by URI $url: ${e.message}", warn = true)
            null
        }

    suspend fun getBody(url: String, useProxy: Boolean = false, id: String): String? =
        try {
            getResponseSpec(url, useProxy, id)
                ?.bodyToMono<String>()
                ?.timeout(getRequestTimeout(url))
                ?.awaitFirstOrNull()
        } catch (e: Exception) {
            logMetaLoading(id, "failed to get properties by URI $url: ${e.message}", warn = true)
            null
        }

    suspend fun getEtag(url: String, useProxy: Boolean = false, id: String): String? =
        getHeaders(url = url, useProxy = useProxy, id = id)
            ?.getFirst("etag")
            ?.replace("\"", "")

    fun getOpenSeaUrl(): String = externalHttpClient.openseaUrl  // TODO Temporary

    private fun getRequestTimeout(url: String) =
        if (externalHttpClient.isOpensea(url)) apiOpenseaTimeout else apiDefaultTimeout

    private suspend fun getResponseSpec(url: String, useProxy: Boolean = false, id: String): WebClient.ResponseSpec? {
        if (url.isBlank()) return null

        return try {
            externalHttpClient.get(url = url, useProxy = useProxy)
        } catch (e: Exception) {
            logMetaLoading(id, "failed to parse URI: $url: ${e.message}", warn = true)
            null
        }
    }
}
