package com.rarible.core.loader

import com.rarible.core.daemon.sequential.ConsumerWorkerHolder
import com.rarible.core.loader.internal.KafkaLoadTaskId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class LoadContextIt : AbstractIntegrationTest() {

    @Autowired
    lateinit var loadWorkers: ConsumerWorkerHolder<KafkaLoadTaskId>

    @Autowired
    lateinit var loadNotificationListenersWorkers: ConsumerWorkerHolder<LoadNotification>

    @Test
    fun `all workers are started`() {
        assertThat(loadWorkers.isActive).isTrue
        assertThat(loadNotificationListenersWorkers.isActive).isTrue
    }
}
