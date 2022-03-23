package com.rarible.core.content.meta.loader

import io.ktor.client.call.*
import io.ktor.client.features.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.client.engine.cio.CIO
import io.ktor.http.*
import io.ktor.utils.io.*
import io.ktor.utils.io.core.*
import java.net.URL
import io.ktor.client.HttpClient

class KtorApacheClientContentReceiver(
    private val timeout: Int,
    private val threadsCount: Int = 64
) : ContentReceiver {

    private val client = HttpClient(CIO) {
        engine {
            threadsCount = this@KtorApacheClientContentReceiver.threadsCount
            maxConnectionsCount = 1000
            requestTimeout = 10000
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
            val contentType = httpResponse.headers[HttpHeaders.ContentType]
            ContentBytes(bytes, contentType, httpResponse.contentLength())
        }
    }

    override fun close() {
        client.close()
    }
}
