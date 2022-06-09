package com.rarible.loader.cache.internal

import com.rarible.loader.cache.CacheType
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.ReactiveMongoOperations
import org.springframework.data.mongodb.core.find
import org.springframework.data.mongodb.core.findById
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.inValues
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

    suspend fun <T> getAll(type: CacheType, keys: List<String>): List<MongoCacheEntry<T>> {
        val query = Query(Criteria("_id").inValues(keys))
        return mongo.find<MongoCacheEntry<T>>(query, getCacheCollection(type))
            .collectList()
            .awaitSingle()
    }

    suspend fun <T> findAll(
        type: CacheType,
        fromId: String?,
        limit: Int
    ): List<MongoCacheEntry<T>> {
        val criteria = fromId?.let {
            Criteria.where("_id").gt(it)
        } ?: Criteria()

        val query = Query(criteria)
            .with(Sort.by("_id"))
            .limit(limit)

        return mongo.find<MongoCacheEntry<T>>(query, getCacheCollection(type))
            .collectList()
            .awaitSingle()
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

    companion object {

        fun getCacheCollection(cacheType: CacheType): String = "cache-$cacheType"
    }
}
