package com.rarible.core.meta.resource.detector.core

import com.rarible.core.meta.resource.detector.ContentBytes
import com.rarible.core.meta.resource.detector.ContentMeta

class ContentMetaDetectProcessor(
    private val provider: ContentMetaDetectorProvider
) {

    fun detect(contentBytes: ContentBytes): ContentMeta? {
        for (detector in provider.detectors) {
            detector.detect(contentBytes)
                ?.let { return it }
        }
        return null
    }
}

interface ContentMetaDetectorProvider {
    val detectors: List<ContentMetaDetector>
}

class DefaultContentMetaDetectorProvider(
    htmlDetector: HtmlDetector,
    svgDetector: SvgDetector,
    pngDetector: PngDetector,
    exifDetector: ExifDetector
) : ContentMetaDetectorProvider {

    override val detectors: List<ContentMetaDetector> =
        listOf(
            htmlDetector,  // HTML should be BEFORE SVG since svg could be a part of HTML document
            svgDetector,
            pngDetector,
            exifDetector
        )
}
