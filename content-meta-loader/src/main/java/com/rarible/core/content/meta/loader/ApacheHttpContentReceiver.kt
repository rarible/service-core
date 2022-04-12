package com.rarible.core.content.meta.loader

import kotlinx.coroutines.future.await
import org.apache.http.HttpResponse
import org.apache.http.HttpVersion
import org.apache.http.client.config.RequestConfig
import org.apache.http.client.methods.HttpGet
import org.apache.http.client.methods.HttpUriRequest
import org.apache.http.concurrent.FutureCallback
import org.apache.http.conn.ConnectionKeepAliveStrategy
import org.apache.http.impl.client.DefaultConnectionKeepAliveStrategy
import org.apache.http.impl.nio.client.HttpAsyncClients
import org.apache.http.impl.nio.conn.PoolingNHttpClientConnectionManager
import org.apache.http.impl.nio.reactor.DefaultConnectingIOReactor
import org.apache.http.message.BasicHttpResponse
import org.apache.http.message.BasicStatusLine
import org.apache.http.nio.IOControl
import org.apache.http.nio.client.methods.AsyncByteConsumer
import org.apache.http.nio.client.methods.HttpAsyncMethods
import org.apache.http.nio.reactor.ConnectingIOReactor
import org.apache.http.protocol.HttpContext
import java.io.Closeable
import java.io.IOException
import java.net.URL
import java.nio.ByteBuffer
import java.util.concurrent.CancellationException
import java.util.concurrent.CompletableFuture
import java.util.concurrent.atomic.AtomicReference

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

        val promise = CompletableFuture<ContentBytes>().exceptionally { throwable ->
            if ((throwable is CancellationException || throwable.cause is CancellationException)) {
                request.abortedSafely()
            }
            null
        }
        val callback = HttpResponseFutureCallback(promise, request)
        val consumerCallback = HttpAsyncResponseConsumerCallback(url, promise, request, maxBytes)
        client.execute(HttpAsyncMethods.create(request), consumerCallback, callback)
        return promise.await()
    }

    override fun close() {
        client.close()
    }

    private class HttpAsyncResponseConsumerCallback(
        private val url: URL,
        private val promise: CompletableFuture<ContentBytes>,
        private val request: HttpUriRequest,
        maxBytes: Int
    ) : AsyncByteConsumer<HttpResponse>(maxBytes) {

        private val contentBytes = AtomicReference(ContentBytes.EMPTY)
        private val result = AtomicReference(EMPTY_HTTP_RESPONSE)
        private val byteBuffer = ByteBuffer.allocate(maxBytes)

        override fun onResponseReceived(response: HttpResponse) {
            try {
                result.set(response)
                val entity = response.entity
                val code = response.statusLine.statusCode
                if (entity != null && code == 200) {
                    val contentLength = entity.contentLength
                    val contentType = entity.contentType?.value

                    contentBytes.set(ContentBytes.EMPTY.copy(
                        url = url,
                        contentType = contentType,
                        contentLength = contentLength.takeUnless { it < 0 }
                    ))
                } else {
                    completeExceptionally(IOException("No response entity, http code=$code"))
                }
            } catch (ex: Throwable) {
                completeExceptionally(ex)
            }
        }

        override fun onByteReceived(buf: ByteBuffer, ioControl: IOControl) {
            byteBuffer.put(buf.array(), 0, minOf(buf.limit(), byteBuffer.remaining()))
            val currentContentBytes = contentBytes.get()
            contentBytes.set(currentContentBytes.copy(bytes = byteBuffer.array().copyOf(byteBuffer.position())))

            if (byteBuffer.remaining() == 0) {
                complete()
            }
        }

        override fun buildResult(context: HttpContext): HttpResponse? {
            complete()
            return null
        }

        private fun complete() {
            promise.complete(contentBytes.get())
            request.abortedSafely()
        }

        private fun completeExceptionally(ex: Throwable) {
            promise.completeExceptionally(ex)
            request.abortedSafely()
        }
    }

    private class HttpResponseFutureCallback(
        private val promise: CompletableFuture<ContentBytes>,
        private val request: HttpUriRequest
    ) : FutureCallback<HttpResponse> {

        override fun completed(httpResponse: HttpResponse) {
            if (promise.isDone.not()) promise.complete(ContentBytes.EMPTY)
        }

        override fun failed(ex: Exception) {
            if (promise.isDone.not()) promise.completeExceptionally(ex)
        }

        override fun cancelled() {
            request.abortedSafely()
            if (promise.isDone.not()) promise.completeExceptionally(
                CancellationException("Request ${request.uri} was canceled")
            )
        }
    }
}

internal val EMPTY_HTTP_RESPONSE: HttpResponse = BasicHttpResponse(
    BasicStatusLine(HttpVersion.HTTP_1_1, 500, "No server response")
)

internal fun HttpUriRequest.abortedSafely() {
    if (!isAborted) {
        abort()
    }
}