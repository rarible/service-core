package com.rarible.core.content.meta.loader

import com.rarible.core.common.nowMillis
import com.rarible.core.content.meta.loader.detector.ExifDetector
import com.rarible.core.content.meta.loader.detector.HtmlDetector
import com.rarible.core.content.meta.loader.detector.PngDetector
import com.rarible.core.content.meta.loader.detector.SvgDetector
import org.slf4j.LoggerFactory
import java.net.URL
import java.time.Duration
import java.time.Instant

class ContentMetaReceiver(
    private val contentReceiver: ContentReceiver,
    private val maxBytes: Int,
    private val contentReceiverMetrics: ContentReceiverMetrics
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    @Suppress("BlockingMethodInNonBlockingContext")
    suspend fun receive(url: String): ContentMeta? {
        return try {
            receive(URL(url))
        } catch (e: Throwable) {
            logger.warn("Wrong URL: $url", e)
            null
        }
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

    private fun getFallbackContentMeta(url: URL, contentBytes: ContentBytes?): ContentMeta? {
        val contentType = contentBytes?.contentType
        if (contentType != null && knownMediaTypePrefixes.any { contentType.startsWith(it) }) {
            val fallback = ContentMeta(
                type = contentType,
                size = contentBytes.contentLength
            )
            logger.info("${logPrefix(url)}: falling back by mimeType from the HTTP headers to $fallback")
            return fallback
        }
        val extension = url.toExternalForm().substringAfterLast(".", "")
        val mimeType = extensionMapping[extension]
        if (mimeType != null) {
            val fallback = ContentMeta(
                type = mimeType,
                size = contentBytes?.contentLength
            )
            logger.info("${logPrefix(url)}: falling back by extension to $fallback")
            return fallback
        }
        logger.warn("${logPrefix(url)}: cannot fall back (contentType = $contentType, extension = $extension)")
        return null
    }

    private fun getPredefinedContentMeta(url: URL): ContentMeta? {
        val extension = url.toExternalForm().substringAfterLast(".")
        val mediaType = ignoredExtensions[extension] ?: return null
        return ContentMeta(mediaType)
    }

    private suspend fun doReceive(url: URL): ContentMeta? {
        val startLoading = nowMillis()

        val contentBytes = try {
            contentReceiver.receiveBytes(url, maxBytes)
        } catch (e: Exception) {
            logger.warn("${logPrefix(url)}: failed to received content bytes (spent ${spent(startLoading)})", e)
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

        // HTML should be BEFORE SVG since svg could be a part of HTML document
        HtmlDetector.detect(contentBytes)?.let { return it }
        SvgDetector.detect(contentBytes)?.let { return it }

        PngDetector.detect(contentBytes)?.let { return it }
        ExifDetector.detect(contentBytes)?.let { return it }

        return getFallbackContentMeta(url, contentBytes)
    }

    private companion object {

        private val slowThreshold = Duration.ofSeconds(1)

        val ignoredExtensions = mapOf(
            "mp3" to "audio/mp3",
            "wav" to "audio/wav",
            "flac" to "audio/flac",
            "mpga" to "audio/mpeg",

            "gltf" to "model/gltf+json",
            "glb" to "model/gltf-binary"
        )

        val extensionMapping = mapOf(
            "png" to "image/png",
            "jpg" to "image/jpeg",
            "jpeg" to "image/jpeg",
            "gif" to "image/gif",
            "bmp" to "image/bmp",
            "mp4" to "video/mp4",
            "webm" to "video/webm",
            "avi" to "video/x-msvideo",
            "mpeg" to "video/mpeg"
        )

        val knownMediaTypePrefixes = listOf("image/", "video/", "audio/", "model/")
    }

    private fun countResult(contentMeta: ContentMeta?) {
        if (contentMeta != null && contentMeta.type.isNotBlank()) {
            contentReceiverMetrics.receiveContentMetaTypeSuccess()
        } else {
            contentReceiverMetrics.receiveContentMetaTypeFail()
        }
        if (contentMeta != null && (contentMeta.width != null && contentMeta.height != null)) {
            contentReceiverMetrics.receiveContentMetaWidthHeightSuccess()
        } else {
            contentReceiverMetrics.receiveContentMetaWidthHeightFail()
        }
    }

    private fun logPrefix(url: URL): String = "Content meta by $url"

    private fun spent(duration: Duration): String {
        return duration.presentableSlow(slowThreshold)
    }

    private fun spent(from: Instant): String {
        return Duration.between(from, nowMillis()).presentableSlow(slowThreshold)
    }
}
