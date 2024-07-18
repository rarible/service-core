package com.rarible.core.content.meta.loader.resolver

import com.rarible.core.content.meta.loader.ContentMetaResult
import com.rarible.core.meta.resource.model.ContentData
import com.rarible.core.meta.resource.model.ContentMeta
import java.net.URI

abstract class ContentMetaResolver(
    private val approach: String
) {

    protected abstract fun resolveContent(uri: URI, data: ContentData?): ContentMeta?

    fun resolve(
        uri: URI,
        data: ContentData? = null,
        exception: Throwable? = null
    ): ContentMetaResult? {
        val result = resolveContent(uri, data) ?: return null

        return ContentMetaResult(
            meta = result.copy(available = result.available || data != null),
            approach = approach,
            bytesRead = data?.getReadBytesSize() ?: 0,
            exception = exception
        )
    }
}
