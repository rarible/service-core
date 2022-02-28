package com.rarible.core.content.meta.loader

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.apache.*
import io.ktor.client.features.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.utils.io.*
import io.ktor.utils.io.core.*
import java.net.URL

class KtorApacheClientContentReceiver(
    private val timeout: Int,
    private val threadsCount: Int = 16
) : ContentReceiver {

    private val client = HttpClient(Apache) {
        engine {
            threadsCount = this@KtorApacheClientContentReceiver.threadsCount

            followRedirects = true
            socketTimeout = timeout
            connectTimeout = timeout
            connectionRequestTimeout = timeout
            customizeClient {
                setMaxConnTotal(1000)
                setMaxConnPerRoute(100)
            }
        }

        BrowserUserAgent()
    }

    override suspend fun receiveBytes(url: URL, maxBytes: Int): ContentBytes {
        return client.get<HttpStatement>(url).execute { httpResponse ->
            val channel: ByteReadChannel = httpResponse.receive()
            val bytes = try {
                channel.readRemaining(maxBytes.toLong()).readBytes()
            } finally {
                channel.cancel()
            }
            val contentLength = httpResponse.headers[HttpHeaders.ContentLength]?.toLongOrNull()
            val contentType = httpResponse.headers[HttpHeaders.ContentType]
            ContentBytes(bytes, contentType, contentLength)
        }
    }

    override fun close() {
        client.close()
    }
}
