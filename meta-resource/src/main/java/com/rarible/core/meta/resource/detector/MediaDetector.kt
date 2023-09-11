package com.rarible.core.meta.resource.detector

import com.rarible.core.meta.resource.model.ContentData
import com.rarible.core.meta.resource.model.ContentMeta

interface MediaDetector {

    // entityId used for logging only
    fun detect(contentBytes: ContentData, entityId: String): ContentMeta?

    fun logPrefix(id: String): String = "Content meta by $id"
}
