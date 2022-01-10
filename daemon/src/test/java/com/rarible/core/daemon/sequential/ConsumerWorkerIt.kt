package com.rarible.core.daemon.sequential

import com.rarible.core.daemon.DaemonWorkerProperties
import com.rarible.core.daemon.RetryProperties
import com.rarible.core.kafka.KafkaMessage
import com.rarible.core.kafka.RaribleKafkaConsumer
import com.rarible.core.kafka.RaribleKafkaProducer
import com.rarible.core.kafka.json.JsonDeserializer
import com.rarible.core.kafka.json.JsonSerializer
import com.rarible.core.test.containers.KafkaTestContainer
import com.rarible.core.test.data.randomString
import com.rarible.core.test.wait.BlockingWait
import com.rarible.core.test.wait.Wait
import io.mockk.Answer
import io.mockk.ManyAnswersAnswer
import io.mockk.ThrowingAnswer
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CompletionHandler
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.apache.kafka.clients.consumer.OffsetResetStrategy
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.time.Duration

data class TestObject(val field1: String, val field2: Int)

@Disabled
class ConsumerWorkerIt {
    private val kafkaContainer = KafkaTestContainer()

    private val topic = "test-topic-${System.currentTimeMillis()}"
    private val producer = RaribleKafkaProducer(
        clientId = "test-producer",
        valueSerializerClass = JsonSerializer::class.java,
        valueClass = TestObject::class.java,
        defaultTopic = topic,
        bootstrapServers = kafkaContainer.kafkaBoostrapServers()
    )
    private val consumer = RaribleKafkaConsumer(
        clientId = "test-consumer",
        consumerGroup = "test-group",
        valueDeserializerClass = JsonDeserializer::class.java,
        valueClass = TestObject::class.java,
        defaultTopic = topic,
        bootstrapServers = kafkaContainer.kafkaBoostrapServers(),
        offsetResetStrategy = OffsetResetStrategy.EARLIEST
    )

    @Test
    fun `handle events`() {
        val handled = arrayListOf<TestObject>()
        val consumerWorker = ConsumerWorker(
            consumer = consumer,
            eventHandler = object : ConsumerEventHandler<TestObject> {
                override suspend fun handle(event: TestObject) {
                    handled += event
                }
            },
            workerName = "worker",
            properties = DaemonWorkerProperties(),
            retryProperties = RetryProperties()
        )
        val testObjects = (1..100).map { TestObject(randomString(), 1) }
        runBlocking {
            producer.send(testObjects.map { KafkaMessage(it.field1, it) }).collect()
        }
        consumerWorker.start()
        BlockingWait.waitAssert {
            assertThat(handled).isEqualTo(testObjects)
        }
        consumerWorker.close()
    }

    @Test
    fun `handle exception - fail 5 out of 6 times - but then handle success`() {
        val eventHandler = mockk<ConsumerEventHandler<TestObject>>()
        val exception = RuntimeException()
        coEvery { eventHandler.handle(any()) } answers(repeatAnswer(5, ThrowingAnswer(exception) )) andThen Unit
        val consumerWorker = ConsumerWorker(
            consumer = consumer,
            eventHandler = eventHandler,
            workerName = "worker",
            properties = DaemonWorkerProperties(
                errorDelay = Duration.ZERO, // Retry to handle a single event immediately.
            ),
            retryProperties = RetryProperties(attempts = 6)
        )
        val testObject = TestObject(randomString(), 1)
        runBlocking {
            producer.send(KafkaMessage(testObject.field1, testObject))
        }
        consumerWorker.start()
        BlockingWait.waitAssert {
            coVerify(exactly = 6) { eventHandler.handle(any()) }
        }
        consumerWorker.close()
    }

    @Test
    fun `fatal exception - do not acknowledge an element`() {
        val eventHandler = mockk<ConsumerEventHandler<TestObject>>()
        val error = OutOfMemoryError("test")
        coEvery { eventHandler.handle(any()) } throws error
        val completionHandler = mockk<CompletionHandler>()
        justRun { completionHandler.invoke(error) }
        val consumerWorker = ConsumerWorker(
            consumer = consumer,
            eventHandler = eventHandler,
            workerName = "worker",
            completionHandler = completionHandler
        )
        val testObject = TestObject(randomString(), 1)
        runBlocking {
            producer.send(KafkaMessage(testObject.field1, testObject))
        }
        consumerWorker.start()
        BlockingWait.waitAssert {
            verify(exactly = 1) { completionHandler.invoke(error) }
        }
        consumerWorker.close()

        // The event must be processed the next time.
        val anotherWorker = ConsumerWorker(
            consumer = consumer,
            eventHandler = eventHandler,
            workerName = "anotherWorker",
        )
        clearMocks(eventHandler)
        coJustRun { eventHandler.handle(testObject) }
        anotherWorker.start()
        BlockingWait.waitAssert {
            coVerify(exactly = 1) { eventHandler.handle(testObject) }
        }
        anotherWorker.close()
    }

    @Test
    fun `handle batch events`() {
        val handled = arrayListOf<List<TestObject>>()
        val eventHandler = object : ConsumerBatchEventHandler<TestObject> {
            override suspend fun handle(event: List<TestObject>) {
                handled += event
            }
        }
        val consumerWorker = ConsumerBatchWorker(
            consumer = consumer,
            eventHandler = eventHandler,
            workerName = "worker",
            properties = DaemonWorkerProperties(),
        )
        val testObjects = (1..100).map { TestObject(randomString(), 1) }
        runBlocking {
            producer.send(testObjects.map { KafkaMessage(it.field1, it) }).collect()
        }
        consumerWorker.start()
        BlockingWait.waitAssert {
            assertThat(handled).isEqualTo(listOf(testObjects))
        }
        consumerWorker.close()
    }

    @Test
    fun `handle batch exception - fail 5 out of 6 times - but then handle success`() {
        val eventHandler = mockk<ConsumerBatchEventHandler<TestObject>>()
        val exception = RuntimeException()
        coEvery { eventHandler.handle(any()) } answers(repeatAnswer(5, ThrowingAnswer(exception) )) andThen Unit
        val consumerWorker = ConsumerBatchWorker(
            consumer = consumer,
            eventHandler = eventHandler,
            workerName = "worker",
            properties = DaemonWorkerProperties(
                errorDelay = Duration.ZERO, // Retry to handle events immediately.
            ),
            retryProperties = RetryProperties(attempts = 6)
        )
        val testObjects = (1..100).map { TestObject(randomString(), 1) }
        runBlocking {
            producer.send(testObjects.map { KafkaMessage(it.field1, it) }).collect()
        }
        consumerWorker.start()
        BlockingWait.waitAssert {
            coVerify(exactly = 6) { eventHandler.handle(testObjects) }
        }
        consumerWorker.close()
    }

    @Suppress("SameParameterValue")
    private fun <T> repeatAnswer(times: Int, answer: Answer<T>): Answer<T> =
        ManyAnswersAnswer((0 until times).map { answer })
}
