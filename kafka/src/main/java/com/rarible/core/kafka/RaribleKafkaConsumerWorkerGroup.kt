package com.rarible.core.kafka

class RaribleKafkaConsumerWorkerGroup<T>(
    private val consumers: Collection<RaribleKafkaConsumerWorker<T>>
) : RaribleKafkaConsumerWorker<T> {

    override fun start() {
        consumers.forEach { it.start() }
    }

    override fun close() {
        consumers.forEach { it.close() }
    }
}
