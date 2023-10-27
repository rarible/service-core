package com.rarible.core.content.meta.loader

import com.rarible.core.content.meta.loader.resolver.ByHttpContentTypeResolver
import com.rarible.core.content.meta.loader.resolver.ByUrlExtensionContentTypeResolver
import com.rarible.core.content.meta.loader.resolver.ContentMetaResolver
import com.rarible.core.content.meta.loader.resolver.ExifContentTypeResolver
import com.rarible.core.content.meta.loader.resolver.PredefinedContentTypeResolver
import com.rarible.core.meta.resource.detector.ContentDetector
import com.rarible.core.meta.resource.model.ContentData
import java.net.URL

class ContentMetaReceiver(
    private val contentReceiver: ContentReceiver,
    private val maxBytes: Int,
    contentDetector: ContentDetector
) {

    private val resolvers: List<ContentMetaResolver> = listOf(
        ExifContentTypeResolver(contentDetector),
        ByHttpContentTypeResolver(),
        ByUrlExtensionContentTypeResolver()
    )

    private val predefined: ContentMetaResolver = PredefinedContentTypeResolver()

    suspend fun receive(url: URL): ContentMetaResult {
        // Predefined content like mp3/wav etc., nothing to fetch here
        predefined.resolve(url)?.let { return it }

        return try {
            resolve(url, data = fetchBytes(url))
        } catch (e: Throwable) {
            resolve(url, exception = e)
        }
    }

    private fun resolve(
        url: URL,
        data: ContentData? = null,
        exception: Throwable? = null
    ): ContentMetaResult {
        return resolvers.firstNotNullOfOrNull { it.resolve(url, data, exception) }
            ?: ContentMetaResult(
                meta = null,
                approach = "stub",
                bytesRead = data?.getReadBytesSize() ?: 0,
                exception = exception
            )
    }

    private suspend fun fetchBytes(url: URL): ContentData {
        val data = contentReceiver.receiveBytes(url, maxBytes)

        // Sometimes there is no content-length header, but if file is small,
        // we can assume readBytes == size
        if (data.getReadBytesSize() < maxBytes && data.size == null) {
            return data.copy(size = data.getReadBytesSize().toLong())
        }

        return data
    }
}
