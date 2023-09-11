package com.rarible.core.content.meta.loader

import io.ktor.client.features.BrowserUserAgent
import io.ktor.client.HttpClient
import io.ktor.client.engine.apache.Apache
import org.apache.http.conn.ConnectionKeepAliveStrategy
import org.apache.http.impl.client.DefaultConnectionKeepAliveStrategy

class KtorApacheClientContentReceiver(
    private val timeout: Int,
    private val threadsCount: Int = 1,
    private val totalConnection: Int = 1000,
    private val connectionPerRoute: Int = 20,
    private val keepAlive: Boolean = true
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
                setKeepAliveStrategy(
                    if (keepAlive) DefaultConnectionKeepAliveStrategy.INSTANCE else ConnectionKeepAliveStrategy { _, _ -> -1 }
                )
            }
        }
        BrowserUserAgent()
    }
}
