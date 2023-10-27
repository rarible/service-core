package com.rarible.core.meta.resource.model

@Suppress("ArrayInDataClass")
data class ContentData(
    val data: ByteArray,
    val mimeType: String?,
    val size: Long?
) {

    constructor(data: ByteArray) : this(
        data,
        null,
        data.size.toLong()
    )

    fun getReadBytesSize() = data.size

    fun isEmpty() = data.isEmpty()

    companion object {

        val EMPTY = ContentData(
            ByteArray(0),
            null,
            null
        )
    }
}
