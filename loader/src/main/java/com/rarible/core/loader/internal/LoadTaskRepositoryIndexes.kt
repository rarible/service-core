package com.rarible.core.loader.internal

import kotlinx.coroutines.reactive.awaitFirst
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.ReactiveMongoOperations
import org.springframework.data.mongodb.core.index.Index

object LoadTaskRepositoryIndexes {

    private val logger = LoggerFactory.getLogger(LoadTaskRepositoryIndexes::class.java)

    suspend fun ensureIndexes(mongo: ReactiveMongoOperations) {
        val collection = MongoLoadTaskRepository.COLLECTION
        logger.info("Ensuring Mongo indexes on $collection")
        val indexOps = mongo.indexOps(collection)
        val failedAtAttributePath = LoadTask::status.name + "." + LoadTask.Status.Failed::failedAt.name
        val retryAtAttributePath = LoadTask::status.name + "." + LoadTask.Status.WaitsForRetry::retryAt.name
        val indexes = listOf(
            Index()
                .on(failedAtAttributePath, Sort.Direction.ASC)
                .on("_id", Sort.Direction.ASC)
                .sparse(),

            Index()
                .on(retryAtAttributePath, Sort.Direction.ASC)
                .on("_id", Sort.Direction.ASC)
                .sparse(),

            Index()
                .on(LoadTask::type.name, Sort.Direction.ASC)
                .on(LoadTask::key.name, Sort.Direction.ASC)
        )
        indexes.forEach { index ->
            logger.info("Ensuring Mongo index ${index.indexKeys.keys} on $collection")
            indexOps.ensureIndex(index).awaitFirst()
        }
    }
}
