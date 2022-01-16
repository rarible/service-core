package com.rarible.core.loader.internal

import com.rarible.core.kafka.KafkaMessage
import com.rarible.core.kafka.RaribleKafkaProducer
import com.rarible.core.loader.LoadType
import org.slf4j.LoggerFactory

class LoadTaskKafkaSender(
    private val kafkaSenders: Map<LoadType, RaribleKafkaProducer<KafkaLoadTaskId>>
) {
    private val logger = LoggerFactory.getLogger(LoadTaskKafkaSender::class.java)

    suspend fun send(loadTask: LoadTask) {
        val loadTaskId = loadTask.id
        val loadType = loadTask.type
        val kafkaSender = kafkaSenders[loadType]
        if (kafkaSender == null) {
            logger.warn("No loader found for $loadType")
            return
        }
        kafkaSender.send(
            KafkaMessage(
                key = loadTaskId,
                value = KafkaLoadTaskId(id = loadTaskId)
            )
        ).ensureSuccess()
    }
}
