package com.rarible.core.kafka

import kotlinx.coroutines.flow.Flow
import java.time.Duration

interface KafkaConsumer<V> {
    fun receiveManualAcknowledge(): Flow<KafkaMessage<V>>
    fun receive(): Flow<KafkaMessage<V>>
}
