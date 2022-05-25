package com.rarible.core.meta.resource.detector.ethereum

abstract class ContentDetector(
    protected val url: String
) {
    abstract fun canDecode(): Boolean

    abstract fun getData(): String

    abstract fun getMimeType(): String

    abstract fun getDecodedData(): ByteArray?
}
