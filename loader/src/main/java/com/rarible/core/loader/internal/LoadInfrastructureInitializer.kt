package com.rarible.core.loader.internal

import com.rarible.core.daemon.sequential.ConsumerWorkerHolder
import com.rarible.core.kafka.RaribleKafkaTopics
import com.rarible.core.loader.LoadNotification
import com.rarible.core.loader.LoadType
import com.rarible.core.loader.Loader
import com.rarible.core.loader.configuration.LoadProperties
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.data.mongodb.core.ReactiveMongoOperations
import org.springframework.stereotype.Component

@Component
class LoadInfrastructureInitializer(
    private val mongo: ReactiveMongoOperations,

    private val loadKafkaTopicsRegistry: LoadKafkaTopicsRegistry,
    private val loaders: List<Loader>,
    private val loadWorkers: ConsumerWorkerHolder<KafkaLoadTaskId>,
    private val loadNotificationListenersWorkers: ConsumerWorkerHolder<LoadNotification>
) {

    @EventListener(ApplicationReadyEvent::class)
    fun startInfrastructure() {
        val loadTypes = loaders.map { it.type }
        loadKafkaTopicsRegistry.createTopics(loadTypes)
        createMongoIndexes()
        loadWorkers.start()
        loadNotificationListenersWorkers.start()
    }

    private fun createMongoIndexes() {
        runBlocking {
            LoadTaskRepositoryIndexes.ensureIndexes(mongo)
        }
    }

}
