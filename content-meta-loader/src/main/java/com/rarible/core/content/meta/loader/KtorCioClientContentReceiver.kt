package com.rarible.core.content.meta.loader

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.features.*

class KtorCioClientContentReceiver (
    private val timeout: Int,
    private val threadsCount: Int = 1,
    private val totalConnection: Int = 1000
) : KtorClientContentReceiver() {

    override val client = HttpClient(CIO) {
        engine {
            threadsCount = this@KtorCioClientContentReceiver.threadsCount
            maxConnectionsCount = totalConnection
            requestTimeout = timeout.toLong()
        }
        BrowserUserAgent()
    }

}