package com.rarible.core.kafka

@Deprecated("Use KafkaConsumerFactory")
data class KafkaMessageBatch<V>(
    val messages: List<KafkaMessage<V>>
) {
    fun acknowledge() {
        messages.forEach { it.receiverRecord?.receiverOffset()?.acknowledge() }
    }
}
