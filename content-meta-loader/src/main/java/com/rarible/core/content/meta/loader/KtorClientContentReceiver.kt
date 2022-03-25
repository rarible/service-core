package com.rarible.core.content.meta.loader

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.utils.io.*
import io.ktor.utils.io.core.*
import java.net.URL

abstract class KtorClientContentReceiver : ContentReceiver {

    protected abstract val client: HttpClient

    override suspend fun receiveBytes(url: URL, maxBytes: Int): ContentBytes {
        return client.get<HttpStatement>(url).execute { httpResponse ->
            val channel: ByteReadChannel = httpResponse.receive()
            val bytes = try {
                channel.readRemaining(maxBytes.toLong()).readBytes()
            } finally {
                channel.cancel()
            }
            val contentType = httpResponse.contentType().toString()
            ContentBytes(bytes, contentType, httpResponse.contentLength())
        }
    }

    override fun close() {
        client.close()
    }
}