package com.rarible.core.mongowatcher

import com.mongodb.client.model.changestream.OperationType
import com.rarible.core.test.ext.MongoCleanup
import com.rarible.core.test.ext.MongoTest
import com.rarible.core.test.wait.Wait.waitAssert
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.runBlocking
import org.apache.commons.lang3.RandomUtils
import org.assertj.core.api.Assertions.assertThat
import org.bson.Document
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.mongodb.core.ReactiveMongoOperations
import org.springframework.data.mongodb.core.query.Query
import org.springframework.test.context.ContextConfiguration

@MongoTest
@MongoCleanup
@SpringBootTest(
    properties = [
        "rarible.task.initialDelay = 0"
    ]
)
@Disabled
@ContextConfiguration(classes = [MockContext::class])
class MongoChangeHandlerTest {
    @Autowired
    private lateinit var handler: MockChangeHandler
    @Autowired
    private lateinit var mongo: ReactiveMongoOperations

    @BeforeEach
    fun cleanup() {
        handler.events.clear()
    }

    @Test
    fun saveEvent() = runBlocking {
        val value = RandomUtils.nextInt()
        mongo.save(Document("key", value), "table").awaitFirst()
        waitAssert {
            assertThat(handler.events)
                .hasSize(1)
            val event = handler.events.first()
            assertThat(event.fullDocument)
                .satisfies {
                    assertThat(it["key"])
                        .isEqualTo(value)
                }
        }
    }

    @Test
    fun removeEvent() = runBlocking {
        val value = RandomUtils.nextInt()
        mongo.save(Document("key", value), "table").awaitFirst()
        waitAssert {
            assertThat(handler.events).hasSize(1)
        }
        handler.events.clear()
        mongo.remove(Query(), "table").awaitFirst()
        waitAssert {
            assertThat(handler.events)
                .hasSize(1)
                .satisfies {
                    assertThat(it.first())
                        .hasFieldOrPropertyWithValue(MongoEvent::operationType.name, OperationType.DELETE)
                }
        }
    }
}