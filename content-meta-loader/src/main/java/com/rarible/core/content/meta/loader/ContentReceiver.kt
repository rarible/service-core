package com.rarible.core.content.meta.loader

import com.rarible.core.meta.resource.model.ContentData
import java.io.Closeable
import java.net.URL

interface ContentReceiver : Closeable {

    suspend fun receiveBytes(url: URL, maxBytes: Int): ContentData
}
