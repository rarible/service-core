package com.rarible.core.daemon.sequential

interface ConsumerEventHandler<T> {
    suspend fun handle(event: T)
}