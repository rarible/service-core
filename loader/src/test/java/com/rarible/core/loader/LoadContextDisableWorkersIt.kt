package com.rarible.core.loader

import com.rarible.core.daemon.sequential.ConsumerWorkerHolder
import com.rarible.core.loader.internal.common.KafkaLoadTaskId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ActiveProfiles

@ActiveProfiles("disable-workers", inheritProfiles = false)
class LoadContextDisableWorkersIt : AbstractIntegrationTest() {
    @Autowired(required = false)
    var loadWorkers: ConsumerWorkerHolder<KafkaLoadTaskId>? = null

    @Autowired
    lateinit var loadNotificationListenersWorkers: ConsumerWorkerHolder<LoadNotification>

    @Test
    fun `notification listeners but not loading workers are initialized`() {
        assertThat(loadWorkers).isNull()
        assertThat(loadNotificationListenersWorkers.isActive).isTrue
    }
}
