package com.rarible.core.kafka

import kotlinx.coroutines.flow.Flow

interface KafkaProducer<V> {
    suspend fun send(message: KafkaMessage<V>, topic: String): KafkaSendResult
    suspend fun send(message: KafkaMessage<V>): KafkaSendResult
    fun send(messages: Flow<KafkaMessage<V>>, topic: String): Flow<KafkaSendResult>
    fun send(messages: Flow<KafkaMessage<V>>): Flow<KafkaSendResult>
    fun send(messages: Collection<KafkaMessage<V>>, topic: String): Flow<KafkaSendResult>
    fun send(messages: Collection<KafkaMessage<V>>): Flow<KafkaSendResult>
}