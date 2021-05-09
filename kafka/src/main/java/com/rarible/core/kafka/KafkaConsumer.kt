package com.rarible.core.kafka

import kotlinx.coroutines.flow.Flow

interface KafkaConsumer<V> {
    fun receiveBatch(topic: String): Flow<Flow<KafkaMessage<V>>>
    fun receiveBatch(): Flow<Flow<KafkaMessage<V>>>
    fun receive(topic: String): Flow<KafkaMessage<V>>
    fun receive(): Flow<KafkaMessage<V>>
}