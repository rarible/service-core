package com.rarible.core.loader.internal

import kotlinx.coroutines.reactive.awaitFirst
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.ReactiveMongoOperations
import org.springframework.data.mongodb.core.index.Index

object LoadTaskRepositoryIndexes {

    private val logger = LoggerFactory.getLogger(LoadTaskRepositoryIndexes::class.java)

    val RESCHEDULED_ATTRIBUTE_PATH = LoadTask::status.name + "." + LoadTask.Status.WaitsForRetry::rescheduled.name
    val RETRY_AT_ATTRIBUTE_PATH = LoadTask::status.name + "." + LoadTask.Status.WaitsForRetry::retryAt.name

    suspend fun ensureIndexes(mongo: ReactiveMongoOperations) {
        val collection = MongoLoadTaskRepository.COLLECTION
        logger.info("Ensuring Mongo indexes on $collection")
        val indexOps = mongo.indexOps(collection)
        val indexes = listOf(
            Index()
                .on(RESCHEDULED_ATTRIBUTE_PATH, Sort.Direction.ASC)
                .on(RETRY_AT_ATTRIBUTE_PATH, Sort.Direction.ASC)
                .on("_id", Sort.Direction.ASC)
                .sparse(),
        )
        indexes.forEach { index ->
            logger.info("Ensuring Mongo index ${index.indexKeys.keys} on $collection")
            indexOps.ensureIndex(index).awaitFirst()
        }
    }
}
