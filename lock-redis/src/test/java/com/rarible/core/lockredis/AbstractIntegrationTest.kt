package com.rarible.core.lockredis

import com.rarible.core.lock.LockService
import com.rarible.core.test.ext.RedisTest
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ContextConfiguration

@ContextConfiguration(classes = [MockConfiguration::class])
@RedisTest
@SpringBootTest
abstract class AbstractIntegrationTest {
    @Autowired
    protected lateinit var lockService: LockService
}