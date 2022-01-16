package com.rarible.core.loader.internal

import com.rarible.core.kafka.KafkaMessage
import com.rarible.core.kafka.RaribleKafkaProducer
import com.rarible.core.loader.LoadNotification

class LoadNotificationKafkaSender(
    private val kafkaSenders: Map<String, RaribleKafkaProducer<LoadNotification>>
) : AutoCloseable {

    suspend fun send(loadNotification: LoadNotification) {
        val kafkaSender = kafkaSenders[loadNotification.type]
            ?: throw LoadFatalError("No notification sender found for ${loadNotification.type}")
        kafkaSender.send(
            KafkaMessage(
                key = loadNotification.taskId,
                value = loadNotification
            )
        ).ensureSuccess()
    }

    override fun close() {
        kafkaSenders.values.forEach { it.close() }
    }
}
