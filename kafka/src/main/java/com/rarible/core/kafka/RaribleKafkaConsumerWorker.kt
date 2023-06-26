package com.rarible.core.kafka

interface RaribleKafkaConsumerWorker<T> : AutoCloseable {

    fun start()
}
