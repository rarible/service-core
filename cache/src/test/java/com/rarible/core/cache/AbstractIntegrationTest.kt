package com.rarible.core.cache

import com.rarible.core.test.base.BaseMongoTest
import com.rarible.core.test.mongo.MongoCleanup
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ContextConfiguration

@MongoCleanup
@SpringBootTest(
    properties = [
        "rarible.cache.use-locks=false"
    ]
)
@ContextConfiguration(classes = [MockContext::class])
abstract class AbstractIntegrationTest : BaseMongoTest()