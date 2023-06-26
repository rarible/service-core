package com.rarible.core.kafka

import com.rarible.core.kafka.json.RARIBLE_KAFKA_CLASS_PARAM
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.transform
import kotlinx.coroutines.reactive.asFlow
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.OffsetResetStrategy
import org.apache.kafka.common.header.Headers
import org.apache.kafka.common.serialization.Deserializer
import org.apache.kafka.common.serialization.StringDeserializer
import reactor.kafka.receiver.KafkaReceiver
import reactor.kafka.receiver.ReceiverOptions
import java.nio.charset.StandardCharsets
import java.util.Locale

@Deprecated("Use KafkaConsumerFactory")
class RaribleKafkaConsumer<V>(
    /**
     * Anything to identify the client
     */
    clientId: String,
    /**
     * Consumers with the same group act like a queue consumers
     */
    consumerGroup: String,
    valueDeserializerClass: Class<out Deserializer<*>>,
    private val defaultTopic: String,
    bootstrapServers: String,
    offsetResetStrategy: OffsetResetStrategy = OffsetResetStrategy.LATEST,
    valueClass: Class<V>? = null,
    autoCreateTopic: Boolean? = null,
    maxPollIntervalMs: Int? = null, //TODO: add a test for this parameter.
    // Deprecated parameter, use explicit parameters or make a PR to the library.
    properties: Map<String, String> = emptyMap()
) : KafkaConsumer<V> {
    private val receiverOptions: ReceiverOptions<String, V>

    init {
        val mapOf: Map<String, Any?> = mapOf(
            ConsumerConfig.CLIENT_ID_CONFIG to clientId,
            ConsumerConfig.GROUP_ID_CONFIG to consumerGroup,
            ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG to StringDeserializer::class.java,
            ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG to valueDeserializerClass,
            RARIBLE_KAFKA_CLASS_PARAM to valueClass,
            ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG to bootstrapServers,
            ConsumerConfig.AUTO_OFFSET_RESET_CONFIG to offsetResetStrategy.name.lowercase(Locale.getDefault()),
            ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG to false,
            ConsumerConfig.MAX_POLL_RECORDS_CONFIG to 500,
            ConsumerConfig.ALLOW_AUTO_CREATE_TOPICS_CONFIG to autoCreateTopic,
            ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG to maxPollIntervalMs
        ) + properties
        receiverOptions = ReceiverOptions.create(mapOf.filterValues { it != null })
    }

    override fun receiveManualAcknowledge(): Flow<KafkaMessage<V>> {
        return KafkaReceiver.create(receiverOptions.subscription(listOf(defaultTopic)))
            .receive()
            .map {
                KafkaMessage(it.key(), it.value(), headers = it.headers().toMap(), receiverRecord = it)
            }.asFlow()
    }

    override fun receiveAutoAck(): Flow<KafkaMessage<V>> {
        return receiveManualAcknowledge().transform { message ->
            emit(message)
            message.receiverRecord?.receiverOffset()?.acknowledge()
        }
    }

    override fun receiveBatchManualAck(maxBatchSize: Int): Flow<KafkaMessageBatch<V>> {
        val flow = receiveManualAcknowledge()
        return flow.chunked(minOf(maxBatchSize, MAX_BATCH_SIZE), 1000).map { KafkaMessageBatch(it) }
    }

    private fun Headers.toMap(): Map<String, String> {
        val headers = HashMap<String, String>()

        forEach {
            headers[it.key()] = it.value().toString(StandardCharsets.UTF_8)
        }
        return headers
    }

    private companion object {
        private const val MAX_BATCH_SIZE = 500
    }
}
