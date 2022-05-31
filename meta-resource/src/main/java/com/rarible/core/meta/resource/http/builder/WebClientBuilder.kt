package com.rarible.core.meta.resource.http.builder

import org.springframework.web.reactive.function.client.WebClient

interface WebClientBuilder {
    fun build(): WebClient
}
