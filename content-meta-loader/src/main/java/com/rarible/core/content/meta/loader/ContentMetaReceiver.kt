package com.rarible.core.content.meta.loader

import com.rarible.core.common.nowMillis
import com.rarible.core.meta.resource.detector.ContentBytes
import com.rarible.core.meta.resource.detector.ContentMeta
import com.rarible.core.meta.resource.detector.MimeType
import com.rarible.core.meta.resource.detector.core.ExifDetector
import com.rarible.core.meta.resource.detector.core.HtmlDetector
import com.rarible.core.meta.resource.detector.core.PngDetector
import com.rarible.core.meta.resource.detector.core.SvgDetector
import org.slf4j.LoggerFactory
import java.net.URL
import java.time.Duration
import java.time.Instant

class ContentMetaReceiver(
    private val contentReceiver: ContentReceiver,
    private val maxBytes: Int,
    private val contentReceiverMetrics: ContentReceiverMetrics
) {

    @Suppress("BlockingMethodInNonBlockingContext")
    suspend fun receive(url: String): ContentMeta? {
        val parsedUrl = try {
            URL(url)
        } catch (e: Throwable) {
            logger.warn("Wrong URL: $url", e)
            return null
        }
        return receive(parsedUrl)
    }

    @Suppress("MemberVisibilityCanBePrivate")
    suspend fun receive(url: URL): ContentMeta? {
        getPredefinedContentMeta(url)?.let { return it }

        logger.info("${logPrefix(url)}: started receiving")
        val startSample = contentReceiverMetrics.startReceiving()
        val result = try {
            val contentMeta = doReceive(url)
            val duration = contentReceiverMetrics.endReceiving(startSample, true)
            if (contentMeta != null) {
                logger.info("${logPrefix(url)}: received $contentMeta (in ${spent(duration)})")
                contentMeta
            } else {
                getFallbackContentMeta(url, null)
            }
        } catch (e: Throwable) {
            val duration = contentReceiverMetrics.endReceiving(startSample, false)
            logger.warn("${logPrefix(url)}: failed to receive (in ${spent(duration)})", e)
            getFallbackContentMeta(url, null)
        }
        countResult(result)
        return result
    }

    private suspend fun doReceive(url: URL): ContentMeta? {
        val startLoading = nowMillis()

        val contentBytes = try {
            contentReceiver.receiveBytes(url, maxBytes)
        } catch (e: Exception) {
            logger.warn("${logPrefix(url)}: failed to receive content bytes (spent ${spent(startLoading)})", e)
            return getFallbackContentMeta(url, null)
        }

        logger.info(
            "${logPrefix(url)}: received content " +
                "(bytes ${contentBytes.bytes.size}, " +
                "content length ${contentBytes.contentLength}, " +
                "mime type ${contentBytes.contentType}) " +
                "in ${spent(startLoading)}"
        )

        contentReceiverMetrics.receivedBytes(contentBytes.bytes.size)

        // HTML should be BEFORE SVG since svg could be a part of HTML document  // TODO add provider
        HtmlDetector.detect(contentBytes)?.let { return it }
        SvgDetector.detect(contentBytes)?.let { return it }

        PngDetector.detect(contentBytes)?.let { return it }
        ExifDetector.detect(contentBytes)?.let { return it }

        return getFallbackContentMeta(url, contentBytes)
    }

    private fun countResult(contentMeta: ContentMeta?) {
        if (contentMeta != null && contentMeta.type.isNotBlank()) {
            contentReceiverMetrics.receiveContentMetaTypeSuccess()
        } else {
            contentReceiverMetrics.receiveContentMetaTypeFail()
        }
        // Should be counted only for video/image, other types doesn't have width/height
        if (contentMeta != null &&
            (contentMeta.type.startsWith("image/") || contentMeta.type.startsWith("video/"))
        ) {
            if (contentMeta.width != null && contentMeta.height != null) {
                contentReceiverMetrics.receiveContentMetaWidthHeightSuccess()
            } else {
                contentReceiverMetrics.receiveContentMetaWidthHeightFail()
            }
        }
    }

    private companion object {

        private val logger = LoggerFactory.getLogger(javaClass)

        private val slowThreshold = Duration.ofSeconds(1)

        private val ignoredExtensions = mapOf(
            "mp3" to MimeType.MP3_AUDIO,
            "wav" to MimeType.WAV_AUDIO,
            "flac" to MimeType.FLAC_AUDIO,
            "mpga" to MimeType.MPEG_AUDIO,
            "gltf" to MimeType.GLTF_JSON_MODEL,
            "glb" to MimeType.GLTF_BINARY_MODEL
        )

        private val extensionMapping = mapOf(
            "png" to MimeType.PNG_IMAGE,
            "jpg" to MimeType.JPEG_IMAGE,
            "jpeg" to MimeType.JPEG_IMAGE,
            "gif" to MimeType.GIF_IMAGE,
            "bmp" to MimeType.BMP_IMAGE,
            "webp" to MimeType.WEBP_IMAGE,
            "mp4" to MimeType.MP4_VIDEO,
            "webm" to MimeType.WEBM_VIDEO,
            "avi" to MimeType.X_MSVIDEO_VIDEO,
            "mpeg" to MimeType.MPEG_VIDEO
        )

        private val knownMediaTypePrefixes = listOf("image/", "video/", "audio/", "model/")

        private fun getPredefinedContentMeta(url: URL): ContentMeta? {
            val mediaType = ignoredExtensions[url.extension()] ?: return null
            return ContentMeta(mediaType.value)
        }

        private fun getFallbackContentMeta(url: URL, contentBytes: ContentBytes?): ContentMeta? {
            resolveByHttpContentType(url, contentBytes)
                ?.let { return it }

            resolveByUrlExtension(url, contentBytes)
                ?.let { return it }

            logger.warn("${logPrefix(url)}: cannot fall back (contentType = ${contentBytes?.contentType}, extension = ${url.extension()})")
            return null
        }

        private fun resolveByHttpContentType(url: URL, contentBytes: ContentBytes?): ContentMeta? {
            val contentType = contentBytes?.contentType
            if (contentType != null && knownMediaTypePrefixes.any { contentType.startsWith(it) }) {
                val fallback = ContentMeta(
                    type = contentType,
                    size = contentBytes.contentLength
                )
                logger.info("${logPrefix(url)}: falling back by mimeType from the HTTP headers to $fallback")
                return fallback
            }
            return null
        }

        private fun resolveByUrlExtension(url: URL, contentBytes: ContentBytes?): ContentMeta? {
            val mimeType = extensionMapping[url.extension()]
            if (mimeType != null) {
                val fallback = ContentMeta(
                    type = mimeType.value,
                    size = contentBytes?.contentLength
                )
                logger.info("${logPrefix(url)}: falling back by extension to $fallback")
                return fallback
            }
            return null
        }

        private fun URL.extension(): String = this.toExternalForm().substringAfterLast(".")

        private fun logPrefix(url: URL): String = "Content meta by $url"

        private fun spent(duration: Duration): String {
            return duration.presentableSlow(slowThreshold)
        }

        private fun spent(from: Instant): String {
            return Duration.between(from, nowMillis()).presentableSlow(slowThreshold)
        }

    }

}
