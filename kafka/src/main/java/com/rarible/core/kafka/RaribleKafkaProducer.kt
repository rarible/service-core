package com.rarible.core.kafka

import com.rarible.core.kafka.json.RARIBLE_KAFKA_CLASS_PARAM
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.single
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactor.asFlux
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.header.Headers
import org.apache.kafka.common.header.internals.RecordHeaders
import org.apache.kafka.common.serialization.Serializer
import org.apache.kafka.common.serialization.StringSerializer
import reactor.kafka.sender.KafkaSender
import reactor.kafka.sender.SenderOptions
import reactor.kafka.sender.SenderRecord
import java.nio.charset.StandardCharsets
import java.time.Duration

/**
 * Thread-safe
 */
class RaribleKafkaProducer<V>(
    /**
     * Anything to identify the client
     */
    clientId: String,
    valueSerializerClass: Class<out Serializer<*>>,
    private val defaultTopic: String,
    bootstrapServers: String,
    /**
     * Max messages in memory
     */
    backpressure: Int = 512,
    acknowledgement: Acknowledgement = Acknowledgement.ALL,
    /**
     * Max time to retry delivery of a message
     */
    deliveryTimeout: Duration = Duration.ofMinutes(2),
    valueClass: Class<V>? = null
) : AutoCloseable, KafkaProducer<V> {

    private val sender: KafkaSender<String, V>

    init {
        val senderProperties: Map<String, Any?> = mapOf(
            ProducerConfig.CLIENT_ID_CONFIG to clientId,
            ProducerConfig.BOOTSTRAP_SERVERS_CONFIG to bootstrapServers,
            ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG to StringSerializer::class.java,
            ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG to valueSerializerClass,
            RARIBLE_KAFKA_CLASS_PARAM to valueClass,
            ProducerConfig.ACKS_CONFIG to acknowledgement.kafkaValue,
            ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG to deliveryTimeout.toMillis().toInt()
        )
        val senderOptions = SenderOptions
            .create<String, V>(senderProperties)
            .maxInFlight(backpressure)

        sender = KafkaSender.create(senderOptions)
    }

    override fun send(messages: Flow<KafkaMessage<V>>, topic: String): Flow<KafkaSendResult> {
        return sender.send(messages.map {
            val record = it.toProducerRecord(topic)
            val correlationId = it.id
            SenderRecord.create(record, correlationId)
        }.asFlux()).map {
            val correlationId = it.correlationMetadata()
            val exception = it.exception()

            if (exception == null) KafkaSendResult.Success(correlationId) else KafkaSendResult.Fail(
                correlationId,
                exception
            )
        }.asFlow()
    }

    override fun send(messages: Collection<KafkaMessage<V>>, topic: String): Flow<KafkaSendResult> {
        return send(messages.asFlow(), topic)
    }

    override suspend fun send(message: KafkaMessage<V>, topic: String): KafkaSendResult {
        return send(listOf(message).asFlow(), topic).single()
    }

    override suspend fun send(message: KafkaMessage<V>): KafkaSendResult = send(message, defaultTopic)

    override fun send(messages: Flow<KafkaMessage<V>>): Flow<KafkaSendResult> = send(messages, defaultTopic)

    override fun send(messages: Collection<KafkaMessage<V>>): Flow<KafkaSendResult> = send(messages, defaultTopic)

    private fun <V> KafkaMessage<V>.toProducerRecord(topic: String): ProducerRecord<String, V> {
        return ProducerRecord(topic, null, key, value, headers.toRecordHeaders())
    }

    private fun Map<String, String>.toRecordHeaders(): Headers? {
        if (this.isEmpty()) return null
        val headers = RecordHeaders()

        forEach { header ->
            headers.add(header.key, header.value.toByteArray(StandardCharsets.UTF_8))
        }
        return headers
    }

    override fun close() {
        sender.close()
    }
}

enum class Acknowledgement(val kafkaValue: String) {
    /**
     * No guarantees (fastest)
     */
    NONE("0"),

    /**
     * Delivery not guaranteed in case of master failure
     */
    MASTER("1"),

    /**
     * Delivery guaranteed (slowest)
     */
    ALL("all")
}