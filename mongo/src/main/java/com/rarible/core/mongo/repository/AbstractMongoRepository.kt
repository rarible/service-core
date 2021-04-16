package com.rarible.core.mongo.repository

import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.springframework.data.mongodb.core.ReactiveMongoOperations
import reactor.core.publisher.Mono

abstract class AbstractMongoRepository<T, Id>(
    protected val mongo: ReactiveMongoOperations,
    protected val entityClass: Class<T>
) {
    fun saveR(t: T): Mono<T> = mongo.save(t)

    suspend fun save(t: T): T = saveR(t).awaitFirst()

    fun findByIdR(id: Id): Mono<T> = mongo.findById(id, entityClass)

    suspend fun findById(id: Id): T? = findByIdR(id).awaitFirstOrNull()
}