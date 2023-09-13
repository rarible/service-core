package com.rarible.core.kafka

import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationContext

class KafkaShutdownHook(
    private val context: ApplicationContext,
    private val delegate: Runnable,
) : Runnable {
    override fun run() {
        context.getBeansOfType(RaribleKafkaConsumerWorker::class.java).values.forEach {
            try {
                it.close()
            } catch (e: Exception) {
                logger.info("Failed to shudtdown kafka consumer $it", e)
            }
        }
        delegate
    }

    companion object {
        private val logger = LoggerFactory.getLogger(KafkaShutdownHook::class.java)
    }
}
