package com.rarible.loader.cache.internal

import com.rarible.core.common.nowMillis
import com.rarible.core.common.optimisticLock
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
    // TODO[loader]: use Clock.
) {

    suspend fun <T> save(
        type: CacheType,
        key: String,
        data: T,
        cachedAt: Instant
    ): CacheRepositorySaveResult<T> {
        val mongoCacheEntry = MongoCacheEntry(
            key = key,
            data = data as Any,
            cachedAt = cachedAt
        )
        val (previousData, newData) = optimisticLock {
            val previousEntry = getCacheEntry<T>(type, key)
            mongo.save(
                mongoCacheEntry.copy(version = previousEntry?.version ?: mongoCacheEntry.version),
                getCacheCollection(type)
            ).awaitSingle()
            @Suppress("UNCHECKED_CAST")
            previousEntry?.data as? T to data
        }
        return CacheRepositorySaveResult(key, previousData, newData)
    }

    suspend fun <T> get(type: CacheType, key: String): MongoCacheEntry<T>? {
        return getCacheEntry(type, key)
    }

    suspend fun contains(type: CacheType, key: String): Boolean =
        mongo.exists(
            Query(Criteria("_id").isEqualTo(key)),
            getCacheCollection(type)
        ).awaitSingle()

    suspend fun remove(type: CacheType, key: String) {
        val query = Query(Criteria("_id").isEqualTo(key))
        mongo.remove(query, getCacheCollection(type)).awaitSingleOrNull()
    }

    private suspend fun <T> getCacheEntry(type: CacheType, key: String) =
        mongo.findById<MongoCacheEntry<T>>(key, getCacheCollection(type))
            .awaitSingleOrNull()

    private fun getCacheCollection(cacheType: CacheType): String = "cache-$cacheType"
}
