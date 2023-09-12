package com.rarible.core.kafka

import org.apache.kafka.clients.consumer.OffsetResetStrategy

data class RaribleKafkaContainerFactorySettings<T>(
    val hosts: String,
    // Number of partition handlers
    val concurrency: Int = 9,
    val batchSize: Int,
    val offsetResetStrategy: OffsetResetStrategy = OffsetResetStrategy.EARLIEST,
    val valueClass: Class<T>,
    val deserializer: Class<*>? = null,
    val shouldSkipEventsOnError: Boolean = false,
    val customSettings: Map<String, Any> = emptyMap()
)
