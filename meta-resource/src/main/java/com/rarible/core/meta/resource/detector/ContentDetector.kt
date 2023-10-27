package com.rarible.core.meta.resource.detector

import com.rarible.core.meta.resource.model.ContentData
import com.rarible.core.meta.resource.model.ContentMeta

class ContentDetector(
    private val mediaDetectors: List<MediaDetector> = listOf(
        HtmlDetector, // HTML should be BEFORE svg since it can contain SVG elements
        SvgDetector,
        PngDetector,
        ExifDetector
    )
) {

    fun detect(contentData: ContentData, entityId: String): ContentMeta? {
        return mediaDetectors.firstNotNullOfOrNull { it.detect(contentData, entityId) }
    }
}
