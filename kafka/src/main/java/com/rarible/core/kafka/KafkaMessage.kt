package com.rarible.core.kafka

import reactor.kafka.receiver.ReceiverRecord

data class KafkaMessage<V>(
    val key: String,
    val value: V,
    val id: String = key,
    val headers: Map<String, String> = emptyMap(),
    val receiverRecord: ReceiverRecord<String, V>? = null
)