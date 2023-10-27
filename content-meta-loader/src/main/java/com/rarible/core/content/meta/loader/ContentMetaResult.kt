package com.rarible.core.content.meta.loader

import com.rarible.core.meta.resource.model.ContentMeta

data class ContentMetaResult(
    val meta: ContentMeta?,
    val approach: String,
    val bytesRead: Int,
    val exception: Throwable?
)
