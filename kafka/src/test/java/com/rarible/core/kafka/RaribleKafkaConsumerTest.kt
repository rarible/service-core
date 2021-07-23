package com.rarible.core.kafka

import com.rarible.core.kafka.json.JsonDeserializer
import com.rarible.core.kafka.json.JsonSerializer
import com.rarible.core.test.containers.KafkaTestContainer
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.time.withTimeout
import org.apache.kafka.clients.consumer.OffsetResetStrategy
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.time.Duration

data class TestObject(val field1: String, val field2: Int)

@FlowPreview
@Disabled
internal class RaribleKafkaConsumerTest {
    private val kafkaContainer = KafkaTestContainer()

    @Test
    fun sendReceiveKafkaMessage() = runBlocking<Unit> {
        val producer = RaribleKafkaProducer(
            clientId = "test-producer",
            valueSerializerClass = JsonSerializer::class.java,
            valueClass = TestObject::class.java,
            defaultTopic = "test-topic",
            bootstrapServers = kafkaContainer.kafkaBoostrapServers()
        )
        val consumer = RaribleKafkaConsumer(
            clientId = "test-consumer",
            consumerGroup = "test-group",
            valueDeserializerClass = JsonDeserializer::class.java,
            valueClass = TestObject::class.java,
            defaultTopic = "test-topic",
            bootstrapServers = kafkaContainer.kafkaBoostrapServers(),
            offsetResetStrategy = OffsetResetStrategy.EARLIEST
        )
        val sendResult = withTimeout(Duration.ofSeconds(5)) {
            val headers = hashMapOf("header1" to "value1", "header2" to "value2")
            producer.send(KafkaMessage(key = "key", value = TestObject("field1", 1), headers = headers))
        }

        assertThat(sendResult.isSuccess).isEqualTo(true)

        val received = withTimeout(Duration.ofSeconds(5)) {
            consumer.receive("test-topic").first()
        }

        assertThat(received.key).isEqualTo("key")
        assertThat(received.value.field1).isEqualTo("field1")
        assertThat(received.value.field2).isEqualTo(1)
        assertThat(received.headers.size).isEqualTo(3)
        assertThat(received.headers["header1"]).isEqualTo("value1")
        assertThat(received.headers["header2"]).isEqualTo("value2")
    }
}