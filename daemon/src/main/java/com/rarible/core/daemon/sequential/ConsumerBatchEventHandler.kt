package com.rarible.core.daemon.sequential

interface ConsumerBatchEventHandler<T> {

    suspend fun handle(event: List<T>)
}