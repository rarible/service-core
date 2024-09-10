package com.rarible.core.meta.resource.model

import kotlinx.coroutines.flow.Flow
import org.springframework.http.MediaType

data class FlowResponse(
    val mediaType: MediaType? = null,
    val body: Flow<ByteArray>? = null,
    val size: Long? = null,
) {
    companion object {
        val EMPTY = FlowResponse()
    }
}
