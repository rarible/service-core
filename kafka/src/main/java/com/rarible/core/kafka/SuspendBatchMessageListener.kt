package com.rarible.core.kafka

import org.apache.kafka.clients.consumer.ConsumerRecord

interface SuspendBatchMessageListener<K, T> {

    suspend fun onMessage(records: List<ConsumerRecord<K, T>>)
}
