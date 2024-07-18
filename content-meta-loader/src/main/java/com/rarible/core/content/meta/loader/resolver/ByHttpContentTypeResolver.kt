package com.rarible.core.content.meta.loader.resolver

import com.rarible.core.meta.resource.model.ContentData
import com.rarible.core.meta.resource.model.ContentMeta
import java.net.URI

class ByHttpContentTypeResolver : ContentMetaResolver(
    "content-type"
) {

    companion object {
        private val knownMediaTypePrefixes = listOf(
            "image/",
            "video/",
            "audio/",
            "model/"
        )
    }

    override fun resolveContent(uri: URI, data: ContentData?): ContentMeta? {
        val contentType = data?.mimeType ?: return null

        if (!knownMediaTypePrefixes.any { contentType.startsWith(it) }) {
            return null
        }
        return ContentMeta(
            mimeType = contentType,
            size = data.size
        )
    }
}
