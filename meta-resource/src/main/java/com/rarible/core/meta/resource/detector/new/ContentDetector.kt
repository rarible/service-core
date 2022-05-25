package com.rarible.core.meta.resource.detector.new

import com.rarible.core.meta.resource.detector.core.ContentBytes
import com.rarible.core.meta.resource.detector.core.ContentMeta

interface ContentDetector {
    fun detect(contentBytes: ContentBytes): ContentMeta?
}

interface ContentDecoder {
    fun getDecodedData(): ByteArray?
}

//
//fun getMimeType(): String
//fun canDecode(): Boolean

//abstract fun getData(): String
//abstract fun getDecodedData(): ByteArray?

