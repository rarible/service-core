package com.rarible.core.content.meta.loader

import com.rarible.core.meta.resource.model.ContentData
import io.ktor.client.HttpClient
import io.ktor.client.call.receive
import io.ktor.client.request.get
import io.ktor.client.statement.HttpStatement
import io.ktor.http.contentType
import io.ktor.http.contentLength
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.cancel
import io.ktor.utils.io.readRemaining
import io.ktor.utils.io.core.readBytes
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
