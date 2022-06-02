package com.rarible.core.meta.resource.detector.embedded

import com.rarible.core.meta.resource.model.ContentData

interface ContentDecoder {

    fun decode(data: String): ContentData?
}
