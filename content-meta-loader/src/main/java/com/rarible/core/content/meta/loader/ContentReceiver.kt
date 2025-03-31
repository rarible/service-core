package com.rarible.core.content.meta.loader

import com.rarible.core.meta.resource.model.ContentData
import java.io.Closeable
import java.net.URI

interface ContentReceiver : Closeable {

    suspend fun receiveBytes(blockchain: String, uri: URI, maxBytes: Int): ContentData
}
