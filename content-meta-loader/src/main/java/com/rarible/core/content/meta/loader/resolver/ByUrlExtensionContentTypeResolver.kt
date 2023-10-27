package com.rarible.core.content.meta.loader.resolver

import com.rarible.core.meta.resource.model.ContentData
import com.rarible.core.meta.resource.model.ContentMeta
import com.rarible.core.meta.resource.model.MimeType
import com.rarible.core.meta.resource.util.extension
import java.net.URL

class ByUrlExtensionContentTypeResolver : ContentMetaResolver("url-extension") {

    companion object {
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
    }

    override fun resolveContent(url: URL, data: ContentData?): ContentMeta? {
        val mimeType = extensionMapping[url.extension()] ?: return null

        return ContentMeta(
            mimeType = mimeType.value,
            size = data?.size
        )
    }
}
