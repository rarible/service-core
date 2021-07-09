package com.rarible.core.reduce

import com.rarible.core.test.containers.MongodbReactiveBaseTest

abstract class AbstractIntegrationTest : MongodbReactiveBaseTest() {
    protected val template = createReactiveMongoTemplate()
}