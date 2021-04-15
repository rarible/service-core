package com.rarible.core.task

import com.rarible.core.test.ext.MongoCleanup
import com.rarible.core.test.ext.MongoTest
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.mongodb.core.ReactiveMongoOperations
import org.springframework.test.context.ContextConfiguration

@MongoTest
@MongoCleanup
@SpringBootTest
@ContextConfiguration(classes = [MockContext::class])
abstract class AbstractIntegrationTest {
    @Autowired
    protected lateinit var mongo: ReactiveMongoOperations

    @Autowired
    protected lateinit var runner: TaskRunner

    @Autowired
    protected lateinit var taskRepository: TaskRepository

    @Autowired
    private lateinit var listener: TaskRunnerEventListener

    @BeforeEach
    fun before() {
        listener.runnerEvents.clear()
    }

    val runnerEvents: List<TaskRunnerEvent>
        get() = listener.runnerEvents
}