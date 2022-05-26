package com.rarible.core.meta.resource.detector.core

import com.rarible.core.meta.resource.detector.ContentBytes
import com.rarible.core.meta.resource.detector.ContentMeta
import java.net.URL

interface ContentMetaDetector {

    fun detect(contentBytes: ContentBytes): ContentMeta?

    fun logPrefix(url: URL): String = "Content meta by $url"

}
