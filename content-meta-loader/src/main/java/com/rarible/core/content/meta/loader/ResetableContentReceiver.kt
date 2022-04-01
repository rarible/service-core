package com.rarible.core.content.meta.loader

import io.ktor.network.sockets.*
import java.io.Closeable
import java.net.URL
import kotlin.system.exitProcess

class ResetableContentReceiver(
    private val delegate: ContentReceiver,
) : ContentReceiver, Closeable {

    override suspend fun receiveBytes(url: URL, maxBytes: Int): ContentBytes {
        return try {
            delegate.receiveBytes(url, maxBytes)
        } catch (ex: SocketTimeoutException) {
            exitProcess(-1)
        } catch (ex: Throwable) {
            throw ex
        }
    }

    override fun close() {
        delegate.close()
    }
}