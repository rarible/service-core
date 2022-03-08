package com.rarible.core.content.meta.loader

import com.drew.imaging.png.PngChunk
import com.drew.imaging.png.PngChunkType
import com.drew.imaging.png.PngHeader
import com.drew.lang.SequentialByteArrayReader

/**
 * Copied from [com.drew.imaging.png.PngMetadataReader].
 *
 * We need to extract width/height of PNG images by first bytes, but the `metadata-extractor` library
 * requires the PNG file to be fully read (including `PngChunkType.IEND` tag, see [com.drew.imaging.png.PngChunkReader]).
 */
object PngDetector {
    fun detectPngContentMeta(contentBytes: ContentBytes): ContentMeta? {
        val reader = SequentialByteArrayReader(contentBytes.bytes)
        reader.isMotorolaByteOrder = true
        val prefix = runCatching { reader.getBytes(PNG_SIGNATURE_BYTES.size) }.getOrNull() ?: return null
        if (!prefix.contentEquals(PNG_SIGNATURE_BYTES)) {
            return null
        }
        while (true) {
            val chunkDataLength = runCatching { reader.int32 }.getOrNull() ?: return null
            if (chunkDataLength < 0) {
                return null
            }
            val chunkTypeBytes = runCatching { reader.getBytes(4) }.getOrNull() ?: return null
            val chunkType = PngChunkType(chunkTypeBytes)
            if (chunkType == PngChunkType.IHDR) {
                val chunkData = runCatching { reader.getBytes(chunkDataLength) }.getOrNull() ?: return null
                val chunk = PngChunk(chunkType, chunkData)
                val pngHeader = runCatching { PngHeader(chunk.bytes) }.getOrNull() ?: return null
                val imageWidth = pngHeader.imageWidth
                val imageHeight = pngHeader.imageHeight
                return ContentMeta(
                    type = "image/png",
                    width = imageWidth,
                    height = imageHeight,
                    size = contentBytes.contentLength
                )
            } else {
                runCatching { reader.skip(chunkDataLength.toLong()) }.getOrNull() ?: return null
            }
        }
    }

    private val PNG_SIGNATURE_BYTES = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)
}
