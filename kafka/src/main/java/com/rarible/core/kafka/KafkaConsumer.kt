package com.rarible.core.kafka

import kotlinx.coroutines.flow.Flow

interface KafkaConsumer<V> {
    fun receiveManualAcknowledge(): Flow<KafkaMessage<V>>
    fun receiveBatchManualAck(maxBatchSize: Int): Flow<KafkaMessageBatch<V>>
    fun receiveAutoAck(): Flow<KafkaMessage<V>>
}
