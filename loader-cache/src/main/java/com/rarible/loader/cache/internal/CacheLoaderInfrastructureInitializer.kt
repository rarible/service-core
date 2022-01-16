package com.rarible.loader.cache.internal

import kotlinx.coroutines.runBlocking
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.data.mongodb.core.ReactiveMongoOperations
import org.springframework.stereotype.Component

@Component
class CacheLoaderInfrastructureInitializer(
    private val mongo: ReactiveMongoOperations
) {
    @EventListener(ApplicationReadyEvent::class)
    fun startInfrastructure() {
        createMongoIndexes()
    }

    private fun createMongoIndexes() {
        runBlocking {
            CacheLoadTaskIdIndexes.ensureIndexes(mongo)
        }
    }
}
