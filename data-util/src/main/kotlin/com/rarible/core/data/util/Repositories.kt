@file:Suppress("unused")

package com.rarible.core.data.util

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull

import org.springframework.data.repository.reactive.ReactiveCrudRepository

suspend fun <T : Any> ReactiveCrudRepository<T, *>.coSave(entity: T): T =
    this.save(entity).awaitSingle()

suspend fun <T, ID : Any> ReactiveCrudRepository<T, ID>.coFindById(id: ID): T? =
    this.findById(id).awaitSingleOrNull()

fun <T : Any> ReactiveCrudRepository<T, *>.coFindAll(): Flow<T> =
    this.findAll().asFlow()

suspend fun <T : Any> ReactiveCrudRepository<T, *>.coSaveAll(entities: Collection<T>): List<T> =
    this.saveAll(entities).collectList().awaitSingle()

suspend fun <T : Any> ReactiveCrudRepository<T, *>.coSaveAll(vararg entities: T): List<T> =
    this.coSaveAll(entities.toList())