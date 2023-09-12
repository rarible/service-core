package com.rarible.core.kafka

data class RaribleKafkaConsumerSettings(
    val topic: String,
    // Group should NOT include env prefix, it will be added automatically
    val group: String,
    // Messages will be grouped by key and handled in async {} block (works only for non-batch EventHandlers)
    val async: Boolean = true,
    val coroutineThreadCount: Int = 1,
)
