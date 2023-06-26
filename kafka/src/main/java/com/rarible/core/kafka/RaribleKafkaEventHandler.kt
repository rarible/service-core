package com.rarible.core.kafka

interface RaribleKafkaEventHandler<B> {

    suspend fun handle(event: B)
}
