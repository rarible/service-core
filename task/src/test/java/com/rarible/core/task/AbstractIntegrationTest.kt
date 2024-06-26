package com.rarible.core.task

import com.rarible.core.test.ext.MongoCleanup
import com.rarible.core.test.ext.MongoTest
import kotlinx.coroutines.FlowPreview
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.mongodb.core.ReactiveMongoOperations

@FlowPreview
@MongoTest
@MongoCleanup
@SpringBootTest
abstract class AbstractIntegrationTest {
    @Autowired
    protected lateinit var mongo: ReactiveMongoOperations

    @Autowired
    protected lateinit var runner: TaskRunner

    @Autowired
    protected lateinit var taskRepository: TaskRepository
}
