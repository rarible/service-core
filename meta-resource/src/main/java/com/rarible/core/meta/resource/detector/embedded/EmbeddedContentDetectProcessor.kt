package com.rarible.core.meta.resource.detector.embedded

class EmbeddedContentDetectProcessor(
    private val provider: DefaultEmbeddedContentDetectorProvider
) {

    fun detect(url: String): EmbeddedContent? {
        for (detector in provider.detectors) {
            detector.getEmbeddedContent(url)
                ?.let { return it }
        }
        return null
    }
}

interface EmbeddedContentDetectorProvider {
    val detectors: List<EmbeddedContentDetector>
}

class DefaultEmbeddedContentDetectorProvider(
    embeddedBase64Detector: EmbeddedBase64Detector,
    embeddedSvgDetector: EmbeddedSvgDetector
) : EmbeddedContentDetectorProvider {

    override val detectors: List<EmbeddedContentDetector> =
        listOf(
            embeddedBase64Detector,
            embeddedSvgDetector
        )
}
