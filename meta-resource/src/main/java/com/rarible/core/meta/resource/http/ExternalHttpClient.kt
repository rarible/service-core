package com.rarible.core.meta.resource.http

import com.rarible.core.meta.resource.metrics.ResponseSizeRecorder
import com.rarible.core.meta.resource.model.FlowResponse
import com.rarible.core.meta.resource.util.MetaLogger.logMetaLoading
import io.micrometer.core.instrument.MeterRegistry
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.core.io.buffer.DataBufferUtils
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import org.springframework.web.reactive.function.client.bodyToMono
import org.springframework.web.reactive.function.client.toEntity
import org.springframework.web.reactive.function.client.toEntityFlux
import java.time.Duration

/**
 * Client responsible for making HTTP requests to external APIs.
 * Handles OpenSea's API separately (by using a dedicated HTTP proxy).
 */

open class ExternalHttpClient(
    private val defaultClient: HttpClient,
    private val proxyClient: HttpClient,
    private val customClients: List<HttpClient>,
    private val onError: (Exception) -> Unit = {},
    monitoredUrls: List<String>,
    meterRegistry: MeterRegistry,
) {

    private val responseSizeRecorder = ResponseSizeRecorder(monitoredUrls, meterRegistry)

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
            onError(e)
            null
        }
    }

    suspend fun getBody(blockchain: String, url: String, useProxy: Boolean = false, id: String): String? {
        val (responseSpec, timeout) = getResponseSpec(url, useProxy, id) ?: return null
        return try {
            val result = responseSpec
                ?.bodyToMono<String>()
                ?.timeout(timeout)
                ?.awaitFirstOrNull()
            result?.let {
                responseSizeRecorder.recordResponseSize(blockchain = blockchain, url = url, responseSize = it.length)
            }
            result
        } catch (e: Exception) {
            logMetaLoading(id, "failed to get properties by URI $url: ${e.message} ${getResponse(e)}", warn = true)
            onError(e)
            null
        }
    }

    suspend fun getBodyBytes(
        blockchain: String,
        url: String,
        useProxy: Boolean = false,
        id: String
    ): Pair<MediaType?, ByteArray?> {
        val (responseSpec, timeout) = getResponseSpec(url, useProxy, id) ?: return null to null
        return try {
            val entity = responseSpec
                ?.toEntity<ByteArray>()
                ?.timeout(timeout)
                ?.awaitFirstOrNull()
            entity?.body?.let {
                responseSizeRecorder.recordResponseSize(blockchain = blockchain, url = url, responseSize = it.size)
            }
            return entity?.headers?.contentType to entity?.body
        } catch (e: Exception) {
            logMetaLoading(id, "failed to get properties by URI $url: ${e.message} ${getResponse(e)}", warn = true)
            onError(e)
            null to null
        }
    }

    suspend fun getBodyFlow(blockchain: String, url: String, useProxy: Boolean = false, id: String): FlowResponse {
        val (responseSpec, timeout) = getResponseSpec(url, useProxy, id) ?: return FlowResponse.EMPTY
        return try {
            val entity = responseSpec
                ?.toEntityFlux<DataBuffer>()
                ?.timeout(timeout)
                ?.awaitSingleOrNull()
            return FlowResponse(
                mediaType = entity?.headers?.contentType,
                size = entity?.headers?.contentLength,
                body = entity?.body
                    ?.doOnDiscard(DataBuffer::class.java, DataBufferUtils::release)
                    ?.map {
                        val array = ByteArray(it.readableByteCount())
                        it.read(array)
                        DataBufferUtils.release(it)
                        responseSizeRecorder.recordResponseSize(
                            blockchain = blockchain,
                            url = url,
                            responseSize = array.size
                        )
                        array
                    }
                    ?.asFlow()
            )
        } catch (e: Exception) {
            logMetaLoading(id, "failed to get properties by URI $url: ${e.message} ${getResponse(e)}", warn = true)
            onError(e)
            FlowResponse.EMPTY
        }
    }

    suspend fun getEtag(url: String, useProxy: Boolean = false, id: String): String? =
        getHeaders(url = url, useProxy = useProxy, id = id)
            ?.getFirst("etag")
            ?.replace("\"", "")

    fun getResponseSpec(
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
            val headers = e.headers.entries.joinToString("; ") { h ->
                "${h.key}=${h.value.joinToString(", ")}"
            }
            " response: ${e.rawStatusCode}: ${e.statusText}, (headers: $headers)"
        } else {
            ""
        }
}
