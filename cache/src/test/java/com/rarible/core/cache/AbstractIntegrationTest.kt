package com.rarible.core.cache

import com.rarible.core.test.ext.MongoCleanup
import com.rarible.core.test.ext.MongoTest
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ContextConfiguration

@MongoTest
@MongoCleanup
@SpringBootTest(
    properties = [
        "rarible.cache.use-locks=false"
    ]
)
@ContextConfiguration(classes = [MockContext::class])
abstract class AbstractIntegrationTest
