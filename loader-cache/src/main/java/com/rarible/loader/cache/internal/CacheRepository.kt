package com.rarible.loader.cache.internal

import com.rarible.loader.cache.CacheType
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.data.mongodb.core.ReactiveMongoOperations
import org.springframework.data.mongodb.core.findById
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.isEqualTo
import org.springframework.stereotype.Component
import java.time.Instant

@Component
class CacheRepository(
    private val mongo: ReactiveMongoOperations
) {

    suspend fun <T> save(
        type: CacheType,
        key: String,
        data: T,
        cachedAt: Instant
    ): MongoCacheEntry<T> {
        val mongoCacheEntry = MongoCacheEntry(
            key = key,
            data = data,
            cachedAt = cachedAt
        )
        return mongo.save(mongoCacheEntry, getCacheCollection(type)).awaitSingle()
    }

    suspend fun <T> get(type: CacheType, key: String): MongoCacheEntry<T>? =
        mongo.findById<MongoCacheEntry<T>>(key, getCacheCollection(type))
            .awaitSingleOrNull()

    suspend fun contains(type: CacheType, key: String): Boolean =
        mongo.exists(
            Query(Criteria("_id").isEqualTo(key)),
            getCacheCollection(type)
        ).awaitSingle()

    suspend fun remove(type: CacheType, key: String) {
        val query = Query(Criteria("_id").isEqualTo(key))
        mongo.remove(query, getCacheCollection(type)).awaitSingleOrNull()
    }

    companion object {
        fun getCacheCollection(cacheType: CacheType): String = "cache-$cacheType"
    }
}
