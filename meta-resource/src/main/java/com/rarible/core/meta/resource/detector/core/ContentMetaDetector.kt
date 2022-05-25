package com.rarible.core.meta.resource.detector.core

import java.net.URL

interface ContentMetaDetector {

    fun detect(contentBytes: ContentBytes): ContentMeta?

    fun logPrefix(url: URL): String = "Content meta by $url"

}
