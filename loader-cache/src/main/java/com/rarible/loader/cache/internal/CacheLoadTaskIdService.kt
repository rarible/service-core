package com.rarible.loader.cache.internal

import com.rarible.core.loader.internal.LoadTaskId
import com.rarible.loader.cache.CacheType
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.bson.types.ObjectId
import org.springframework.data.annotation.Id
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.ReactiveMongoOperations
import org.springframework.data.mongodb.core.find
import org.springframework.data.mongodb.core.findOne
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.isEqualTo
import org.springframework.stereotype.Component

// TODO[loader]: add tests for this repository
@Component
class CacheLoadTaskIdService(
    private val mongo: ReactiveMongoOperations
) {
    suspend fun save(type: CacheType, key: String, loadTaskId: LoadTaskId) {
        val mongoTaskId = MongoCacheLoadTaskId(loadTaskId, type, key)
        mongo.save(mongoTaskId, COLLECTION).awaitFirstOrNull()
    }

    suspend fun getLastTaskId(type: CacheType, key: String): LoadTaskId? {
        val query = Query(
            Criteria().andOperator(
                MongoCacheLoadTaskId::type.isEqualTo(type),
                MongoCacheLoadTaskId::key.isEqualTo(key)
            )
        ).with(Sort.by(Sort.Direction.DESC, "_id")).limit(1)
        return mongo.findOne<MongoCacheLoadTaskId>(query, COLLECTION).awaitFirstOrNull()?.taskId
    }

    companion object {
        const val COLLECTION = "cache-loader-task-ids"
    }
}

// TODO[loader]: add Mongo index for this collection.
@Document
data class MongoCacheLoadTaskId(
    val taskId: LoadTaskId,
    val type: CacheType,
    val key: String,
    @Id
    val id: String = ObjectId().toHexString()
)