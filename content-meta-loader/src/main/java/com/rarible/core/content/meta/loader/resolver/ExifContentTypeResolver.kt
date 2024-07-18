package com.rarible.core.content.meta.loader.resolver

import com.rarible.core.meta.resource.detector.ContentDetector
import com.rarible.core.meta.resource.model.ContentData
import com.rarible.core.meta.resource.model.ContentMeta
import java.net.URI

class ExifContentTypeResolver(
    private val contentDetector: ContentDetector
) : ContentMetaResolver("exif") {

    override fun resolveContent(uri: URI, data: ContentData?): ContentMeta? {
        data ?: return null
        return contentDetector.detect(data, uri.toString())
    }
}
