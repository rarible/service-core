package com.rarible.core.loader.internal

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactor.awaitSingle
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.ReactiveMongoOperations
import org.springframework.data.mongodb.core.find
import org.springframework.data.mongodb.core.findOne
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.isEqualTo
import org.springframework.stereotype.Component
import java.time.Instant

interface LoadTaskRepository {
    fun getAll(): Flow<LoadTask>
    suspend fun get(id: LoadTaskId): LoadTask?
    fun getTasksToRetry(maxRetryAt: Instant): Flow<LoadTask>
    suspend fun save(task: LoadTask): LoadTask
    suspend fun remove(task: LoadTask)
}

// TODO[loader]: improve repo/service tests.
@Component
class MongoLoadTaskRepository(
    private val mongo: ReactiveMongoOperations
) : LoadTaskRepository {

    companion object {
        const val COLLECTION = "loader-tasks"
    }

    override fun getAll(): Flow<LoadTask> {
        // TODO[loader]: fix sorting.
        val query = Query()
        return mongo.find<LoadTask>(query, COLLECTION).asFlow()
    }

    override suspend fun get(id: LoadTaskId): LoadTask? {
        val query = Query(Criteria("_id").isEqualTo(id))
        return mongo.findOne<LoadTask>(query, COLLECTION).awaitFirstOrNull()
    }

    override fun getTasksToRetry(maxRetryAt: Instant): Flow<LoadTask> {
        val retryAtAttributePath = LoadTask::status.name + "." + LoadTask.Status.WaitsForRetry::retryAt.name
        val rescheduledAttributePath = LoadTask::status.name + "." + LoadTask.Status.WaitsForRetry::rescheduled.name
        val query = Query(
                Criteria(retryAtAttributePath).exists(true).lt(maxRetryAt)
                .and(rescheduledAttributePath).isEqualTo(false)
        ).with(Sort.by(Sort.Direction.ASC, retryAtAttributePath, "_id"))
        return mongo.find<LoadTask>(query, COLLECTION).asFlow()
    }

    override suspend fun save(task: LoadTask): LoadTask =
        mongo.save(task, COLLECTION).awaitFirst()

    override suspend fun remove(task: LoadTask) {
        mongo.remove(task, COLLECTION).awaitSingle()
    }
}
