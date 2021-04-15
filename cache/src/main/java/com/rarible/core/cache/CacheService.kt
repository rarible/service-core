package com.rarible.core.cache

import com.rarible.core.common.justOrEmpty
import com.rarible.core.common.orNull
import com.rarible.core.common.toOptional
import com.rarible.core.lock.LockService
import com.rarible.core.logging.LoggingUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.slf4j.Marker
import org.springframework.beans.factory.annotation.Value
import org.springframework.dao.DuplicateKeyException
import org.springframework.dao.OptimisticLockingFailureException
import org.springframework.data.mongodb.core.ReactiveMongoOperations
import org.springframework.data.mongodb.core.findById
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import java.util.*

@Service
class CacheService(
    private val mongo: ReactiveMongoOperations,
    private val lockService: LockService,
    @Value("\${rarible.cache.use-locks}") private val useLocks: Boolean
) {
    fun <T: Any> getCached(
        id: String,
        d: CacheDescriptor<T>,
        immediatelyIfCached: Boolean = false
    ): Mono<T> {
        return LoggingUtils.withMarker { marker ->
            mongo.findById<Cache>(id, d.collection)
                .toOptional()
                .flatMap {
                    val value = it.orNull()
                    when {
                        value == null -> getCachedInternalSync(marker, id, d, "initial load")
                        value.canBeUsed(d.getMaxAge(value.data as T?)) -> value.data.justOrEmpty()
                        value.data != null -> {
                            getCachedInternalSync(marker, id, d, "refreshing cache").subscribe(
                                {},
                                { ex -> logger.error("Unable to refresh cache for $id in ${d.collection}", ex) }
                            )
                            value.data.justOrEmpty()
                        }
                        else -> getCachedInternalSync(marker, id, d, "cache is empty")
                    }
                }
        }
    }

    private fun <T: Any> getCachedInternalSync(
        marker: Marker,
        id: String,
        d: CacheDescriptor<T>,
        description: String
    ): Mono<T> {
        return if (useLocks) {
            lockService.synchronize("cache_${d.collection}_$id", 10000, getCachedInternal(marker, id, d, description))
        } else {
            getCachedInternal(marker, id, d, description)
        }
    }

    private fun <T: Any> getCachedInternal(
        marker: Marker,
        id: String,
        d: CacheDescriptor<T>,
        description: String
    ): Mono<T> {
        return mongo.findById<Cache>(id, d.collection)
            .toOptional()
            .flatMap {
                val cache = it.orNull()
                when {
                    cache != null && cache.canBeUsed(d.getMaxAge(cache.data as T?)) -> {
                        cache.data.justOrEmpty()
                    }
                    else -> {
                        logger.info(marker, "$description: getting $id from ${d.collection}. fetching")
                        getAndUpdateCache(marker, cache, id, d.collection) { d.get(id) }
                    }
                }
            }
    }

    private fun <T: Any> getAndUpdateCache(
        marker: Marker,
        cache: Cache?,
        id: String,
        collection: String,
        get: () -> Mono<T>
    ): Mono<T> {
        logger.info(marker, "getAndUpdateCache $id cached: ${cache != null}")
        return get()
            .toOptional()
            .flatMap { fetched ->
                mongo.save(updateCache(cache, id, fetched.orNull()), collection)
                    .onErrorResume { ex ->
                        when (ex) {
                            is DuplicateKeyException -> Mono.empty()
                            is OptimisticLockingFailureException -> Mono.empty()
                            else -> Mono.error(ex)
                        }
                    }
                    .then(Mono.justOrEmpty(fetched.orNull()))
            }
    }

    private fun <T> updateCache(cache: Cache?, id: String, data: T?): Cache {
        return cache?.copy(data = data, updateDate = Date()) ?: Cache(id, data, updateDate = Date())
    }

    companion object {
        val logger: Logger = LoggerFactory.getLogger(CacheService::class.java)
    }
}

fun <T: Any> CacheService?.get(id: String, d: CacheDescriptor<T>, immediatelyIfCached: Boolean = false): Mono<T> {
    return this?.getCached(id, d, immediatelyIfCached) ?: d.get(id)
}

