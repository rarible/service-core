package com.rarible.core.meta.resource.detector.embedded

class EmbeddedContentDetectProcessor(
    private val provider: DefaultEmbeddedContentDecoderProvider
) {

    fun decode(url: String): EmbeddedContent? {
        for (detector in provider.decoders) {
            detector.getEmbeddedContent(url)
                ?.let { return it }
        }
        return null
    }
}

interface EmbeddedContentDecoderProvider {
    val decoders: List<EmbeddedContentDecoder>
}

class DefaultEmbeddedContentDecoderProvider(
    embeddedBase64Decoder: EmbeddedBase64Decoder,
    embeddedSvgDecoder: EmbeddedSvgDecoder
) : EmbeddedContentDecoderProvider {

    override val decoders: List<EmbeddedContentDecoder> =
        listOf(
            embeddedBase64Decoder,
            embeddedSvgDecoder
        )
}
