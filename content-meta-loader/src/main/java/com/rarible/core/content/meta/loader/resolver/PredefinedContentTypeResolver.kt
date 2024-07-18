package com.rarible.core.content.meta.loader.resolver

import com.rarible.core.meta.resource.model.ContentData
import com.rarible.core.meta.resource.model.ContentMeta
import com.rarible.core.meta.resource.model.MimeType
import com.rarible.core.meta.resource.util.extension
import java.net.URI

class PredefinedContentTypeResolver : ContentMetaResolver("predefined") {

    companion object {
        private val ignoredExtensions = mapOf(
            "mp3" to MimeType.MP3_AUDIO,
            "wav" to MimeType.WAV_AUDIO,
            "flac" to MimeType.FLAC_AUDIO,
            "mpga" to MimeType.MPEG_AUDIO,
            "gltf" to MimeType.GLTF_JSON_MODEL,
            "glb" to MimeType.GLTF_BINARY_MODEL
        )
    }

    override fun resolveContent(uri: URI, data: ContentData?): ContentMeta? {
        val mediaType = ignoredExtensions[uri.extension()] ?: return null
        return ContentMeta(
            mimeType = mediaType.value,
            available = true
        )
    }
}
