package com.rarible.core.content.meta.loader.resolver

import com.rarible.core.content.meta.loader.ContentMetaResult
import com.rarible.core.meta.resource.model.ContentData
import com.rarible.core.meta.resource.model.ContentMeta
import java.net.URL

abstract class ContentMetaResolver(
    private val approach: String
) {

    protected abstract fun resolveContent(url: URL, data: ContentData?): ContentMeta?

    fun resolve(
        url: URL,
        data: ContentData? = null,
        exception: Throwable? = null
    ): ContentMetaResult? {
        val result = resolveContent(url, data) ?: return null

        return ContentMetaResult(
            meta = result.copy(available = result.available || data != null),
            approach = approach,
            bytesRead = data?.getReadBytesSize() ?: 0,
            exception = exception
        )
    }
}
