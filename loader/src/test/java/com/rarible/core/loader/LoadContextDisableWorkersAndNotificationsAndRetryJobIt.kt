package com.rarible.core.loader

import com.rarible.core.daemon.sequential.ConsumerWorkerHolder
import com.rarible.core.loader.internal.common.KafkaLoadTaskId
import com.rarible.core.loader.internal.common.RetryTasksSchedulerSpringJob
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ActiveProfiles

@ActiveProfiles("disable-workers-and-notifications-and-retry-job", inheritProfiles = false)
class LoadContextDisableWorkersAndNotificationsAndRetryJobIt : AbstractIntegrationTest() {
    @Autowired(required = false)
    var loadWorkers: ConsumerWorkerHolder<KafkaLoadTaskId>? = null

    @Autowired(required = false)
    var loadNotificationListenersWorkers: ConsumerWorkerHolder<LoadNotification>? = null

    @Autowired(required = false)
    var retryTasksSchedulerSpringJob: RetryTasksSchedulerSpringJob? = null

    @Test
    fun `notification listeners but not loading workers are initialized`() {
        assertThat(loadWorkers).isNull()
        assertThat(loadNotificationListenersWorkers).isNull()
        assertThat(retryTasksSchedulerSpringJob).isNull()
    }
}
