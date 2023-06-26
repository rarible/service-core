package com.rarible.core.kafka

import org.apache.kafka.clients.consumer.OffsetResetStrategy

data class RaribleKafkaConsumerSettings<T>(
    val hosts: String,
    val topic: String,
    // Group should NOT include env prefix, it will be added automatically
    val group: String,
    // Number of partition handlers
    val concurrency: Int,
    val batchSize: Int,
    // Messages will be grouped by key and handled in async {} block (works only for non-batch EventHandlers)
    val async: Boolean = true,
    val offsetResetStrategy: OffsetResetStrategy = OffsetResetStrategy.EARLIEST,
    val valueClass: Class<T>,
)