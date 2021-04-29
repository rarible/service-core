package com.rarible.core.kafka

import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.reactive.asFlow
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.OffsetResetStrategy
import org.apache.kafka.common.header.Headers
import org.apache.kafka.common.serialization.Deserializer
import org.apache.kafka.common.serialization.StringDeserializer
import reactor.kafka.receiver.KafkaReceiver
import reactor.kafka.receiver.ReceiverOptions
import java.nio.charset.StandardCharsets

class RaribleKafkaConsumer<V>(
    /**
     * Anything to identify the client
     */
    clientId: String,
    /**
     * Consumers with the same group act like a queue consumers
     */
    consumerGroup: String,
    valueDeserializerClass: Class<out Deserializer<V>>,
    private val defaultTopic: String,
    bootstrapServers: String,
    offsetResetStrategy: OffsetResetStrategy = OffsetResetStrategy.LATEST
) {
    private val receiverOptions: ReceiverOptions<String, V>

    init {
        val receiverProperties: Map<String, Any> = mapOf(
            ConsumerConfig.CLIENT_ID_CONFIG to clientId,
            ConsumerConfig.GROUP_ID_CONFIG to consumerGroup,
            ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG to StringDeserializer::class.java,
            ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG to valueDeserializerClass,
            ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG to bootstrapServers,
            ConsumerConfig.AUTO_OFFSET_RESET_CONFIG to offsetResetStrategy.name.toLowerCase()
        )
        receiverOptions = ReceiverOptions.create<String, V>(receiverProperties)
    }

    fun receiveBatch(topic: String = defaultTopic): Flow<Flow<KafkaMessage<V>>> {
        return KafkaReceiver.create(receiverOptions.subscription(listOf(topic)))
            .receiveAutoAck()
            .map { batch ->
                batch.map {
                    KafkaMessage(it.key(), it.value(), headers = it.headers().toMap())
                }.asFlow()
            }.asFlow()
    }

    private fun Headers.toMap(): Map<String, String> {
        val headers = HashMap<String, String>()

        forEach {
            headers[it.key()] = it.value().toString(StandardCharsets.UTF_8)
        }
        return headers
    }

    @FlowPreview
    fun receive(topic: String = defaultTopic): Flow<KafkaMessage<V>> {
        return receiveBatch(topic).flatMapConcat { it }
    }
}
