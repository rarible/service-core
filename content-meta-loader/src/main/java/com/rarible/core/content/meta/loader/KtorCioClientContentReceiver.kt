package com.rarible.core.content.meta.loader

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.features.*

class KtorCioClientContentReceiver (
    private val timeout: Int,
    private val threadsCount: Int = 64
) : KtorClientContentReceiver() {

    override val client = HttpClient(CIO) {
        engine {
            threadsCount = this@KtorCioClientContentReceiver.threadsCount
            maxConnectionsCount = 1000
            requestTimeout = timeout.toLong()
        }
        BrowserUserAgent()
    }

}