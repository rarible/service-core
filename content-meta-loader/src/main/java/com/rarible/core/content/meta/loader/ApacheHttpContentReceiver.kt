package com.rarible.core.content.meta.loader

import kotlinx.coroutines.future.await
import org.apache.http.client.config.RequestConfig
import org.apache.http.client.methods.HttpGet
import org.apache.http.client.methods.HttpUriRequest
import org.apache.http.concurrent.FutureCallback
import org.apache.http.conn.ConnectionKeepAliveStrategy
import org.apache.http.impl.client.DefaultConnectionKeepAliveStrategy
import org.apache.http.impl.nio.client.HttpAsyncClients
import org.apache.http.impl.nio.conn.PoolingNHttpClientConnectionManager
import org.apache.http.impl.nio.reactor.DefaultConnectingIOReactor
import org.apache.http.nio.reactor.ConnectingIOReactor
import org.apache.http.HttpResponse
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.Closeable
import java.io.IOException
import java.io.InputStream
import java.net.URL
import java.util.concurrent.CancellationException
import java.util.concurrent.CompletableFuture

class ApacheHttpContentReceiver(
    private val timeout: Int,
    connectionsPerRoute: Int,
    keepAlive: Boolean
) : ContentReceiver, Closeable {

    private val client = run {
        val reactor: ConnectingIOReactor = DefaultConnectingIOReactor()
        val connectionManager = PoolingNHttpClientConnectionManager(reactor)
        connectionManager.defaultMaxPerRoute = connectionsPerRoute

        val client = HttpAsyncClients.custom()
            .setKeepAliveStrategy(
                if (keepAlive) DefaultConnectionKeepAliveStrategy.INSTANCE
                else ConnectionKeepAliveStrategy { _, _ -> -1 }
            )
            .setConnectionManager(connectionManager)
            .build()

        client.start()
        client
    }

    override suspend fun receiveBytes(url: URL, maxBytes: Int): ContentBytes {
        val request = HttpGet(url.toURI())
        val config: RequestConfig = RequestConfig.custom()
            .setSocketTimeout(timeout)
            .setConnectTimeout(timeout)
            .setConnectionRequestTimeout(timeout)
            .build()
        request.config = config;

        val callback = HttpResponseFutureCallback(request, maxBytes)
        client.execute(request, callback)
        return callback.promise.await()
    }

    override fun close() {
        client.close()
    }

    private class HttpResponseFutureCallback(
        private val request: HttpUriRequest,
        private val maxBytes: Int
    ) : FutureCallback<HttpResponse> {
        val promise: CompletableFuture<ContentBytes> = CompletableFuture<ContentBytes>()

        init {
            promise.exceptionally { throwable ->
                if ((throwable is CancellationException || throwable.cause is CancellationException) &&
                    !request.isAborted
                ) {
                    request.abort()
                }
                null
            }
        }

        override fun completed(httpResponse: HttpResponse) {
            try {
                val entity = httpResponse.entity
                val code = httpResponse.statusLine.statusCode
                if (entity != null && code == 200) {
                    val contentLength = entity.contentLength
                    val contentType = entity.contentType?.value

                    val bytes = readBytes(entity.content, Math.toIntExact(contentLength), maxBytes)
                    request.abort()

                    promise.complete(ContentBytes(
                        bytes = bytes,
                        contentType = contentType,
                        contentLength = contentLength.takeUnless { it < 0 }
                    ))
                } else {
                    promise.completeExceptionally(
                        IOException("No response entity, http code=$code")
                    )
                }
            } catch (ex: Throwable) {
                promise.completeExceptionally(ex)
            }
        }

        override fun failed(ex: Exception) {
            promise.completeExceptionally(ex)
        }

        override fun cancelled() {
            if (!request.isAborted) {
                request.abort()
            }
        }

        private fun readBytes(input: InputStream, contentLength: Int, maxBytes: Int): ByteArray {
            val length = minOf(
                maxBytes,
                if (contentLength < 0) Int.MAX_VALUE else contentLength,
            )

            val buffer = ByteArray(length)
            input.buffered(length).read(buffer, 0, length)
            return buffer
        }
    }

    private companion object {
        val logger: Logger = LoggerFactory.getLogger(ApacheHttpContentReceiver::class.java)
    }
}