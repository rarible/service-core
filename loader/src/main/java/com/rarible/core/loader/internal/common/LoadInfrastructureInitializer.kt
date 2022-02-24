package com.rarible.core.loader.internal.common

import com.rarible.core.loader.Loader
import kotlinx.coroutines.runBlocking
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.data.mongodb.core.ReactiveMongoOperations
import org.springframework.stereotype.Component

@Component
class LoadInfrastructureInitializer(
    private val mongo: ReactiveMongoOperations,
    private val loadKafkaTopicsRegistry: LoadKafkaTopicsRegistry,
    private val loaders: List<Loader>,
) {

    @EventListener(ApplicationReadyEvent::class)
    fun startInfrastructure() {
        val loadTypes = loaders.map { it.type }
        loadKafkaTopicsRegistry.createTopics(loadTypes)
        createMongoIndexes()
    }

    private fun createMongoIndexes() {
        runBlocking {
            LoadTaskRepositoryIndexes.ensureIndexes(mongo)
        }
    }

}
