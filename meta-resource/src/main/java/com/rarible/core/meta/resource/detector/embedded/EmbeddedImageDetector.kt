package com.rarible.core.meta.resource.detector.embedded

class EmbeddedImageDetector(
    private val provider: DefaultEmbeddedImageDetectorProvider
) {

    fun getEmbeddedContent(url: String): EmbeddedContent? {
        for (detector in provider.detectors) {
            detector.getEmbeddedContent(url)
                ?.let { return it }
        }
        return null
    }
}

interface EmbeddedImageDetectorProvider {
    val detectors: List<EmbeddedContentDetector>
}

class DefaultEmbeddedImageDetectorProvider(
    embeddedBase64Detector: EmbeddedBase64Detector,
    embeddedSvgDetector: EmbeddedSvgDetector
) : EmbeddedImageDetectorProvider {

    override val detectors: List<EmbeddedContentDetector> =
        listOf(
            embeddedBase64Detector,
            embeddedSvgDetector
        )
}
