package com.rarible.core.mongo

import com.rarible.core.test.mongo.MongoCleanup
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.mongodb.core.ReactiveMongoOperations
import org.springframework.test.context.ContextConfiguration

@MongoCleanup
@SpringBootTest
@ContextConfiguration(classes = [MockContext::class])
abstract class AbstractIntegrationTest : BaseMongoTest() {
    @Autowired
    protected lateinit var mongo: ReactiveMongoOperations
}