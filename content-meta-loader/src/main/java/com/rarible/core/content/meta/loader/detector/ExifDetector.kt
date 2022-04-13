package com.rarible.core.content.meta.loader.detector

import com.drew.imaging.ImageMetadataReader
import com.drew.metadata.Directory
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
import com.drew.metadata.mov.media.QuickTimeVideoDirectory
import com.drew.metadata.mp3.Mp3Directory
import com.drew.metadata.mp4.media.Mp4VideoDirectory
import com.drew.metadata.photoshop.PsdHeaderDirectory
import com.drew.metadata.png.PngDirectory
import com.drew.metadata.wav.WavDirectory
import com.drew.metadata.webp.WebpDirectory
import com.rarible.core.content.meta.loader.ContentBytes
import com.rarible.core.content.meta.loader.ContentMeta
import org.slf4j.LoggerFactory
import java.net.URL

object ExifDetector : ContentMetaDetector {

    private val logger = LoggerFactory.getLogger(javaClass)

    override fun detect(contentBytes: ContentBytes): ContentMeta? {
        val url = contentBytes.url
        val bytes = contentBytes.bytes
        val metadata = try {
            ImageMetadataReader.readMetadata(bytes.inputStream())
        } catch (e: Exception) {
            logger.warn("${logPrefix(url)}: failed to extract metadata by ${bytes.size} bytes", e)
            return null
        }
        var mimeType: String? = null
        var width: Int? = null
        var height: Int? = null
        var errors = 0
        for (directory in metadata.directories) {
            if (directory is FileTypeDirectory) {
                mimeType = directory.safeString(FileTypeDirectory.TAG_DETECTED_FILE_MIME_TYPE, url)
            }
            parseImageOrVideoWidthAndHeight(directory, url)?.let {
                width = width ?: it.first
                height = height ?: it.second
            }
            errors += directory.errors.toList().size
        }
        val result = ContentMeta(
            type = mimeType ?: contentBytes.contentType ?: return null,
            width = width,
            height = height,
            size = contentBytes.contentLength
        )
        logger.info("${logPrefix(url)}: parsed content meta from EXIF: $result")
        return result
    }

    private fun parseImageOrVideoWidthAndHeight(directory: Directory, url: URL): Pair<Int?, Int?>? {
        return when (directory) {
            // Images
            is JpegDirectory -> {
                directory.safeInt(JpegDirectory.TAG_IMAGE_WIDTH, url) to
                    directory.safeInt(JpegDirectory.TAG_IMAGE_HEIGHT, url)
            }
            is GifImageDirectory -> {
                directory.safeInt(GifImageDirectory.TAG_WIDTH, url) to
                    directory.safeInt(GifImageDirectory.TAG_HEIGHT, url)
            }
            is GifHeaderDirectory -> {
                directory.safeInt(GifHeaderDirectory.TAG_IMAGE_WIDTH, url) to
                    directory.safeInt(GifHeaderDirectory.TAG_IMAGE_HEIGHT, url)
            }
            is BmpHeaderDirectory -> {
                directory.safeInt(BmpHeaderDirectory.TAG_IMAGE_WIDTH, url) to
                    directory.safeInt(BmpHeaderDirectory.TAG_IMAGE_HEIGHT, url)
            }
            is PngDirectory -> {
                directory.safeInt(PngDirectory.TAG_IMAGE_WIDTH, url) to
                    directory.safeInt(PngDirectory.TAG_IMAGE_HEIGHT, url)
            }
            is IcoDirectory -> {
                directory.safeInt(IcoDirectory.TAG_IMAGE_WIDTH, url) to
                    directory.safeInt(IcoDirectory.TAG_IMAGE_HEIGHT, url)
            }
            is PsdHeaderDirectory -> {
                directory.safeInt(PsdHeaderDirectory.TAG_IMAGE_WIDTH, url) to
                    directory.safeInt(PsdHeaderDirectory.TAG_IMAGE_HEIGHT, url)
            }
            is WebpDirectory -> {
                directory.safeInt(WebpDirectory.TAG_IMAGE_WIDTH, url) to
                    directory.safeInt(WebpDirectory.TAG_IMAGE_HEIGHT, url)
            }
            is EpsDirectory -> {
                directory.safeInt(EpsDirectory.TAG_IMAGE_WIDTH, url) to
                    directory.safeInt(EpsDirectory.TAG_IMAGE_HEIGHT, url)
            }
            is ExifImageDirectory -> {
                directory.safeInt(ExifDirectoryBase.TAG_IMAGE_WIDTH, url) to
                    directory.safeInt(ExifDirectoryBase.TAG_IMAGE_HEIGHT, url)
            }
            is HeifDirectory -> {
                directory.safeInt(HeifDirectory.TAG_IMAGE_WIDTH, url) to
                    directory.safeInt(HeifDirectory.TAG_IMAGE_HEIGHT, url)
            }
            // Video
            is QuickTimeVideoDirectory -> {
                directory.safeInt(QuickTimeVideoDirectory.TAG_WIDTH, url) to
                    directory.safeInt(QuickTimeVideoDirectory.TAG_HEIGHT, url)
            }
            is AviDirectory -> {
                directory.safeInt(AviDirectory.TAG_WIDTH, url) to
                    directory.safeInt(AviDirectory.TAG_HEIGHT, url)
            }
            is Mp4VideoDirectory -> {
                directory.safeInt(Mp4VideoDirectory.TAG_WIDTH, url) to
                    directory.safeInt(Mp4VideoDirectory.TAG_HEIGHT, url)
            }
            // Audio
            is Mp3Directory -> null
            is WavDirectory -> null
            else -> null
        }
    }

    private fun Directory.safeInt(tagId: Int, url: URL): Int? = safe(tagId, url) { getInt(tagId) }
    private fun Directory.safeString(tagId: Int, url: URL): String? = safe(tagId, url) { getString(tagId) }

    private fun <T> Directory.safe(tagId: Int, url: URL, parser: () -> T): T? = try {
        parser()
    } catch (e: MetadataException) {
        logger.warn("Failed to parse tag " + getTagName(tagId) + " from $url of parsed to $this", e)
        null
    }
}