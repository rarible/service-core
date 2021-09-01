package com.rarible.core.daemon.sequential

interface BatchConsumerEventHandler<T> {
    suspend fun handle(event: List<T>)
}