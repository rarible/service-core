package com.rarible.core.kafka

import com.rarible.core.test.containers.KafkaTestContainer
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.time.withTimeout
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.common.serialization.StringSerializer
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Duration

@FlowPreview
internal class RaribleKafkaConsumerTest {
    private val kafkaContainer = KafkaTestContainer()

    @Test
    fun sendReceiveKafkaMessage() = runBlocking<Unit> {
        val producer = RaribleKafkaProducer<String>(
            clientId = "test-producer",
            valueSerializerClass = StringSerializer::class.java,
            defaultTopic = "test-topic",
            bootstrapServers = kafkaContainer.kafkaBoostrapServers()
        )
        val consumer = RaribleKafkaConsumer<String>(
            clientId = "test-consumer",
            consumerGroup = "test-group",
            valueDeserializerClass = StringDeserializer::class.java,
            defaultTopic = "test-topic",
            bootstrapServers = kafkaContainer.kafkaBoostrapServers(),
            offsetResetStrategy = OffsetResetStrategy.EARLIEST
        )
        val sendResult = withTimeout(Duration.ofSeconds(5)) {
            val headers = hashMapOf("header1" to "value1", "header2" to "value2")
            producer.send(KafkaMessage(key = "key", value = "value", headers = headers))
        }

        assertThat(sendResult.isSuccess).isEqualTo(true)

        val received = withTimeout(Duration.ofSeconds(5)) {
            consumer.receive("test-topic").first()
        }

        assertThat(received.key).isEqualTo("key")
        assertThat(received.value).isEqualTo("value")
        assertThat(received.headers.size).isEqualTo(2)
        assertThat(received.headers["header1"]).isEqualTo("value1")
        assertThat(received.headers["header2"]).isEqualTo("value2")
    }
}