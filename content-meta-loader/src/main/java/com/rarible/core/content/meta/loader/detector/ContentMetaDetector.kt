package com.rarible.core.content.meta.loader.detector

import com.rarible.core.content.meta.loader.ContentBytes
import com.rarible.core.content.meta.loader.ContentMeta
import java.net.URL

interface ContentMetaDetector {

    fun detect(contentBytes: ContentBytes): ContentMeta?

    fun logPrefix(url: URL): String = "Content meta by $url"

}