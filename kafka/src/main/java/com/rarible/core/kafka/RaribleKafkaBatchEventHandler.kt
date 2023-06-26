package com.rarible.core.kafka

interface RaribleKafkaBatchEventHandler<B> {

    suspend fun handle(event: List<B>)
}
