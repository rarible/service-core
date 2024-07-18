package com.rarible.core.content.meta.loader

import com.rarible.core.content.meta.loader.resolver.ByHttpContentTypeResolver
import com.rarible.core.content.meta.loader.resolver.ByUrlExtensionContentTypeResolver
import com.rarible.core.content.meta.loader.resolver.ContentMetaResolver
import com.rarible.core.content.meta.loader.resolver.ExifContentTypeResolver
import com.rarible.core.content.meta.loader.resolver.PredefinedContentTypeResolver
import com.rarible.core.meta.resource.detector.ContentDetector
import com.rarible.core.meta.resource.model.ContentData
import java.net.URI

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

    suspend fun receive(uri: URI): ContentMetaResult {
        // Predefined content like mp3/wav etc., nothing to fetch here
        predefined.resolve(uri)?.let { return it }

        return try {
            resolve(uri, data = fetchBytes(uri))
        } catch (e: Throwable) {
            resolve(uri, exception = e)
        }
    }

    private fun resolve(
        uri: URI,
        data: ContentData? = null,
        exception: Throwable? = null
    ): ContentMetaResult {
        return resolvers.firstNotNullOfOrNull { it.resolve(uri, data, exception) }
            ?: ContentMetaResult(
                meta = null,
                approach = "stub",
                bytesRead = data?.getReadBytesSize() ?: 0,
                exception = exception
            )
    }

    private suspend fun fetchBytes(uri: URI): ContentData {
        val data = contentReceiver.receiveBytes(uri, maxBytes)

        // Sometimes there is no content-length header, but if file is small,
        // we can assume readBytes == size
        if (data.getReadBytesSize() < maxBytes && data.size == null) {
            return data.copy(size = data.getReadBytesSize().toLong())
        }

        return data
    }
}
