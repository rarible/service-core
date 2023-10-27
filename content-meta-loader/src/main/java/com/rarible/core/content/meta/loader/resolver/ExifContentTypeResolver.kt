package com.rarible.core.content.meta.loader.resolver

import com.rarible.core.meta.resource.detector.ContentDetector
import com.rarible.core.meta.resource.model.ContentData
import com.rarible.core.meta.resource.model.ContentMeta
import java.net.URL

class ExifContentTypeResolver(
    private val contentDetector: ContentDetector
) : ContentMetaResolver("exif") {

    override fun resolveContent(url: URL, data: ContentData?): ContentMeta? {
        data ?: return null
        return contentDetector.detect(data, url.toString())
    }
}
