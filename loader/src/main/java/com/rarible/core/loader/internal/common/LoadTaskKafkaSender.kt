package com.rarible.core.loader.internal.common

import com.rarible.core.kafka.KafkaMessage
import com.rarible.core.kafka.RaribleKafkaProducer
import com.rarible.core.loader.LoadTaskId
import com.rarible.core.loader.LoadType
import org.slf4j.LoggerFactory

class LoadTaskKafkaSender(
    private val kafkaSenders: Map<LoadType, RaribleKafkaProducer<KafkaLoadTaskId>>
) {
    private val logger = LoggerFactory.getLogger(LoadTaskKafkaSender::class.java)

    suspend fun send(loadTaskId: LoadTaskId, loadType: LoadType) {
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
