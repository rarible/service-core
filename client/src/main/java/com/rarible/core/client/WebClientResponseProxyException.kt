package com.rarible.core.client

import org.springframework.web.reactive.function.client.WebClientResponseException

open class WebClientResponseProxyException(
    private val original: WebClientResponseException
) : WebClientResponseException(
    original.message ?: "",
    original.rawStatusCode,
    original.statusText,
    original.headers,
    original.responseBodyAsByteArray,
    null,
    original.request
) {

    open var data: Any? = null
    open var unhandled: Throwable? = null

    override fun getResponseBodyAsString(): String {
        return original.responseBodyAsString
    }
}
