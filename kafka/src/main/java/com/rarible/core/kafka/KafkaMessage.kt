package com.rarible.core.kafka

data class KafkaMessage<V>(
    val key: String,
    val value: V,
    val id: String = key,
    val headers: Map<String, String> = emptyMap()
)