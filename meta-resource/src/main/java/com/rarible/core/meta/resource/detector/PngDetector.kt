package com.rarible.core.meta.resource.detector

import com.drew.imaging.png.PngChunk
import com.drew.imaging.png.PngChunkType
import com.drew.imaging.png.PngHeader
import com.drew.lang.SequentialByteArrayReader
import com.rarible.core.meta.resource.model.ContentData
import com.rarible.core.meta.resource.model.ContentMeta
import com.rarible.core.meta.resource.model.MimeType
import org.slf4j.LoggerFactory

/**
 * Copied from [com.drew.imaging.png.PngMetadataReader].
 *
 * We need to extract width/height of PNG images by first bytes, but the `metadata-extractor` library
 * requires the PNG file to be fully read (including `PngChunkType.IEND` tag, see [com.drew.imaging.png.PngChunkReader]).
 */
object PngDetector : MediaDetector {

    private val logger = LoggerFactory.getLogger(javaClass)

    override fun detect(contentBytes: ContentData, entityId: String): ContentMeta? {
        val reader = SequentialByteArrayReader(contentBytes.data)
        reader.isMotorolaByteOrder = true
        val prefix = runCatching { reader.getBytes(PNG_SIGNATURE_BYTES.size) }.getOrNull() ?: return null
        if (!prefix.contentEquals(PNG_SIGNATURE_BYTES)) {
            return null
        }

        var imageWidth = 0
        var imageHeight = 0
        var imageType = MimeType.PNG_IMAGE
        while (true) {
            val chunkDataLength = runCatching { reader.int32 }.getOrNull() ?: return null
            if (chunkDataLength < 0) {
                return null
            }
            val chunkTypeBytes = runCatching { reader.getBytes(4) }.getOrNull() ?: return null
            when(val chunkType = PngChunkType(chunkTypeBytes)) {
                PngChunkType.IHDR -> {
                    val chunkData = runCatching { reader.getBytes(chunkDataLength) }.getOrNull() ?: return null
                    val chunk = PngChunk(chunkType, chunkData)
                    val pngHeader = runCatching { PngHeader(chunk.bytes) }.getOrNull() ?: return null
                    imageWidth = pngHeader.imageWidth
                    imageHeight = pngHeader.imageHeight
                }
                ACTL_CHUNK_TYPE -> {
                    imageType = MimeType.APNG_IMAGE
                    runCatching { reader.skip(chunkDataLength.toLong()) }.getOrNull() ?: return null
                }
                PngChunkType.IDAT -> break
                else -> {
                    logger.debug("Unexpected chunk type $chunkType, trying to read next chunk")
                    runCatching { reader.skip(chunkDataLength.toLong()) }.getOrNull() ?: return null
                }
            }
            runCatching { reader.skip(4) }.getOrNull() ?: return null // skipping checksum
        }
        val result = ContentMeta(
            mimeType = imageType.value,
            width = imageWidth,
            height = imageHeight,
            size = contentBytes.size
        )
        logger.info("${logPrefix(entityId)}: parsed PNG content meta $result")
        return result
    }

    private val PNG_SIGNATURE_BYTES = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)
    private val ACTL_CHUNK_TYPE = PngChunkType("acTL", true)
}
