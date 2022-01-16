package com.rarible.loader.cache.internal

import kotlinx.coroutines.reactive.awaitFirst
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.ReactiveMongoOperations
import org.springframework.data.mongodb.core.index.Index

object CacheLoadTaskIdIndexes {

    private val logger = LoggerFactory.getLogger(CacheLoadTaskIdIndexes::class.java)

    suspend fun ensureIndexes(mongo: ReactiveMongoOperations) {
        val collection = CacheLoadTaskIdService.COLLECTION
        logger.info("Ensuring Mongo indexes on $collection")
        val indexOps = mongo.indexOps(collection)
        val indexes = listOf(
            Index()
                .on(MongoCacheLoadTaskId::type.name, Sort.Direction.ASC)
                .on(MongoCacheLoadTaskId::key.name, Sort.Direction.ASC)
                .on("_id", Sort.Direction.ASC)
        )
        indexes.forEach { index ->
            logger.info("Ensuring Mongo index ${index.indexKeys.keys} on $collection")
            indexOps.ensureIndex(index).awaitFirst()
        }
    }
}
