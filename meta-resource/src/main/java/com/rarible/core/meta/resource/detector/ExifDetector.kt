package com.rarible.core.meta.resource.detector

import com.drew.imaging.ImageMetadataReader
import com.drew.metadata.Directory
import com.drew.metadata.Metadata
import com.drew.metadata.MetadataException
import com.drew.metadata.avi.AviDirectory
import com.drew.metadata.bmp.BmpHeaderDirectory
import com.drew.metadata.eps.EpsDirectory
import com.drew.metadata.exif.ExifDirectoryBase
import com.drew.metadata.exif.ExifImageDirectory
import com.drew.metadata.file.FileTypeDirectory
import com.drew.metadata.gif.GifHeaderDirectory
import com.drew.metadata.gif.GifImageDirectory
import com.drew.metadata.heif.HeifDirectory
import com.drew.metadata.ico.IcoDirectory
import com.drew.metadata.jpeg.JpegDirectory
import com.drew.metadata.mov.QuickTimeDirectory
import com.drew.metadata.mov.media.QuickTimeVideoDirectory
import com.drew.metadata.mp3.Mp3Directory
import com.drew.metadata.mp4.media.Mp4VideoDirectory
import com.drew.metadata.photoshop.PsdHeaderDirectory
import com.drew.metadata.png.PngDirectory
import com.drew.metadata.wav.WavDirectory
import com.drew.metadata.webp.WebpDirectory
import com.rarible.core.meta.resource.model.ContentData
import com.rarible.core.meta.resource.model.ContentMeta
import org.slf4j.LoggerFactory

object ExifDetector : MediaDetector {

    private val logger = LoggerFactory.getLogger(javaClass)

    private val customDetectors = listOf<CustomDetector>(
        CustomAvifDetector
    )

    override fun detect(contentBytes: ContentData, entityId: String): ContentMeta? {
        val bytes = contentBytes.data
        val metadata = try {
            ImageMetadataReader.readMetadata(bytes.inputStream())
        } catch (e: Exception) {
            // Too many logs
            // logger.warn("${logPrefix(entityId)}: failed to extract metadata by ${bytes.size} bytes", e)
            return null
        }

        detectCustom(contentBytes, metadata)?.let { return it }

        var mimeType: String? = null
        var width: Int? = null
        var height: Int? = null
        var errors = 0
        for (directory in metadata.directories) {
            if (directory is FileTypeDirectory) {
                mimeType = directory.safeString(FileTypeDirectory.TAG_DETECTED_FILE_MIME_TYPE, entityId)
            }
            parseImageOrVideoWidthAndHeight(directory, entityId)?.let {
                width = width ?: it.first
                height = height ?: it.second
            }
            errors += directory.errors.toList().size
        }
        val result = ContentMeta(
            mimeType = mimeType ?: contentBytes.mimeType ?: return null,
            width = width,
            height = height,
            size = contentBytes.size
        )
        logger.info("${logPrefix(entityId)}: parsed content meta from EXIF: $result")
        return result
    }

    private fun detectCustom(contentBytes: ContentData, metadata: Metadata): ContentMeta? {
        customDetectors.forEach { detector ->
            detector.detect(contentBytes, metadata)?.let { return it }
        }
        return null
    }

    private fun parseImageOrVideoWidthAndHeight(directory: Directory, id: String): Pair<Int?, Int?>? {
        return when (directory) {
            // Images
            is JpegDirectory -> {
                directory.safeInt(JpegDirectory.TAG_IMAGE_WIDTH, id) to
                    directory.safeInt(JpegDirectory.TAG_IMAGE_HEIGHT, id)
            }

            is GifImageDirectory -> {
                directory.safeInt(GifImageDirectory.TAG_WIDTH, id) to
                    directory.safeInt(GifImageDirectory.TAG_HEIGHT, id)
            }

            is GifHeaderDirectory -> {
                directory.safeInt(GifHeaderDirectory.TAG_IMAGE_WIDTH, id) to
                    directory.safeInt(GifHeaderDirectory.TAG_IMAGE_HEIGHT, id)
            }

            is BmpHeaderDirectory -> {
                directory.safeInt(BmpHeaderDirectory.TAG_IMAGE_WIDTH, id) to
                    directory.safeInt(BmpHeaderDirectory.TAG_IMAGE_HEIGHT, id)
            }

            is PngDirectory -> {
                directory.safeInt(PngDirectory.TAG_IMAGE_WIDTH, id) to
                    directory.safeInt(PngDirectory.TAG_IMAGE_HEIGHT, id)
            }

            is IcoDirectory -> {
                directory.safeInt(IcoDirectory.TAG_IMAGE_WIDTH, id) to
                    directory.safeInt(IcoDirectory.TAG_IMAGE_HEIGHT, id)
            }

            is PsdHeaderDirectory -> {
                directory.safeInt(PsdHeaderDirectory.TAG_IMAGE_WIDTH, id) to
                    directory.safeInt(PsdHeaderDirectory.TAG_IMAGE_HEIGHT, id)
            }

            is WebpDirectory -> {
                directory.safeInt(WebpDirectory.TAG_IMAGE_WIDTH, id) to
                    directory.safeInt(WebpDirectory.TAG_IMAGE_HEIGHT, id)
            }

            is EpsDirectory -> {
                directory.safeInt(EpsDirectory.TAG_IMAGE_WIDTH, id) to
                    directory.safeInt(EpsDirectory.TAG_IMAGE_HEIGHT, id)
            }

            is ExifImageDirectory -> {
                directory.safeInt(ExifDirectoryBase.TAG_IMAGE_WIDTH, id) to
                    directory.safeInt(ExifDirectoryBase.TAG_IMAGE_HEIGHT, id)
            }

            is HeifDirectory -> {
                directory.safeInt(HeifDirectory.TAG_IMAGE_WIDTH, id) to
                    directory.safeInt(HeifDirectory.TAG_IMAGE_HEIGHT, id)
            }
            // Video
            is QuickTimeVideoDirectory -> {
                directory.safeInt(QuickTimeVideoDirectory.TAG_WIDTH, id) to
                    directory.safeInt(QuickTimeVideoDirectory.TAG_HEIGHT, id)
            }

            is AviDirectory -> {
                directory.safeInt(AviDirectory.TAG_WIDTH, id) to
                    directory.safeInt(AviDirectory.TAG_HEIGHT, id)
            }

            is Mp4VideoDirectory -> {
                directory.safeInt(Mp4VideoDirectory.TAG_WIDTH, id) to
                    directory.safeInt(Mp4VideoDirectory.TAG_HEIGHT, id)
            }
            // Audio
            is Mp3Directory -> null
            is WavDirectory -> null
            else -> null
        }
    }

    private fun Directory.safeInt(tagId: Int, url: String): Int? = safe(tagId, url) { getInt(tagId) }
    private fun Directory.safeString(tagId: Int, url: String): String? = safe(tagId, url) { getString(tagId) }

    private fun <T> Directory.safe(tagId: Int, id: String, parser: () -> T): T? = try {
        parser()
    } catch (e: MetadataException) {
        logger.warn("Failed to parse tag " + getTagName(tagId) + " from $id of parsed to $this", e)
        null
    }

    interface CustomDetector {

        fun detect(contentBytes: ContentData, metadata: Metadata): ContentMeta?
    }

    private object CustomAvifDetector : CustomDetector {

        override fun detect(contentBytes: ContentData, metadata: Metadata): ContentMeta? {
            val quicktimeDirectory = metadata.directories.find { it is QuickTimeDirectory } ?: return null
            if (quicktimeDirectory.getString(QuickTimeVideoDirectory.TAG_MAJOR_BRAND) == "avif") {
                return ContentMeta(
                    // TODO any ways to detect size?
                    mimeType = "image/avif",
                    size = contentBytes.size
                )
            }
            return null
        }
    }
}
