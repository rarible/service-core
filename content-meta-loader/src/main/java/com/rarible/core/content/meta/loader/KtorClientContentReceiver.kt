package com.rarible.core.content.meta.loader

import com.rarible.core.meta.resource.model.ContentData
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

    override suspend fun receiveBytes(url: URL, maxBytes: Int): ContentData {
        return client.get<HttpStatement>(url).execute { httpResponse ->
            val channel: ByteReadChannel = httpResponse.receive()
            val data = try {
                channel.readRemaining(maxBytes.toLong()).readBytes()
            } finally {
                channel.cancel()
            }
            val mimeType = httpResponse.contentType().toString()
            ContentData(data, mimeType, httpResponse.contentLength())
        }
    }

    override fun close() {
        client.close()
    }
}
