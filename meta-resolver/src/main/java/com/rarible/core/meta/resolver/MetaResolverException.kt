package com.rarible.core.meta.resolver

class MetaResolverException(
    override val message: String,
    val status: Status
) : Exception(message) {
    enum class Status {
        CORRUPTED_URL,
        CORRUPTED_JSON,
        TIMEOUT,
        UNKNOWN
    }
}
