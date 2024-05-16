package com.rarible.core.meta.resolver

import com.rarible.core.content.meta.loader.ContentMetaResult
import com.rarible.core.content.meta.loader.resolver.ByHttpContentTypeResolver
import com.rarible.core.content.meta.loader.resolver.ByUrlExtensionContentTypeResolver
import com.rarible.core.content.meta.loader.resolver.ContentMetaResolver
import com.rarible.core.content.meta.loader.resolver.ExifContentTypeResolver
import com.rarible.core.content.meta.loader.resolver.PredefinedContentTypeResolver
import com.rarible.core.meta.resource.detector.ContentDetector
import com.rarible.core.meta.resource.model.ContentData
import java.net.URL

class MetaMediaTypeResolver {

    private val resolvers: List<ContentMetaResolver> = listOf(
        ExifContentTypeResolver(ContentDetector()),
        ByHttpContentTypeResolver(),
        ByUrlExtensionContentTypeResolver()
    )

    private val predefined: ContentMetaResolver = PredefinedContentTypeResolver()

    fun resolveContent(metaUrl: String, rawMeta: RawMeta): ContentMetaResult? {
        val url = URL(metaUrl)
        predefined.resolve(URL(metaUrl))?.let { return it }
        val data = rawMeta.bytes ?: return null

        val contentData = ContentData(
            data = data,
            mimeType = rawMeta.mimeType,
            size = data.size.toLong()
        )

        return resolvers.firstNotNullOfOrNull { it.resolve(url, contentData, null) }
    }
}
