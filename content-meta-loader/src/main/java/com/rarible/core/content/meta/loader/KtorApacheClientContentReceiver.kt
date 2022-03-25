package com.rarible.core.content.meta.loader

import io.ktor.client.features.*
import io.ktor.client.HttpClient
import io.ktor.client.engine.apache.*


class KtorApacheClientContentReceiver(
    private val timeout: Int,
    private val threadsCount: Int = 1,
    private val totalConnection: Int = 1000,
    private val connectionPerRoute: Int = 20
) : KtorClientContentReceiver() {

    override val client = HttpClient(Apache) {
        engine {
            threadsCount = this@KtorApacheClientContentReceiver.threadsCount

            followRedirects = true
            socketTimeout = timeout
            connectTimeout = timeout
            connectionRequestTimeout = timeout
            customizeClient {
                setMaxConnTotal(totalConnection)
                setMaxConnPerRoute(connectionPerRoute)
            }
        }
        BrowserUserAgent()
    }

}
