package com.rarible.core.lockredis

import com.rarible.core.lock.LockService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ContextConfiguration
import ru.roborox.reactive.lock.redis.MockConfiguration

@ContextConfiguration(classes = [MockConfiguration::class])
@SpringBootTest
abstract class AbstractIntegrationTest : BaseRedisTest() {
    @Autowired
    protected lateinit var lockService: LockService
}