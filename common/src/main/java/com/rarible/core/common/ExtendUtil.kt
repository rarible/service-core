package com.rarible.core.common

import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

interface Extended<T> {
    val base: T
}

fun <T, R : Extended<T>> Flux<T>.extend(mapper: (T) -> Mono<R>): Mono<List<R>> {
    return this.collectList()
        .flatMap { it.extend(mapper) }
}

fun <T, R : Extended<T>> List<T>.extend(mapper: (T) -> Mono<R>): Mono<List<R>> {
    return Flux.fromIterable(this)
        .flatMap(mapper)
        .collectList()
        .map { result ->
            val map = result.map { it.base to it }.toMap()
            this.map { map.getValue(it) }
        }
}

interface Identifiable<Id> {
    val id: Id
}

fun <Id, T : Identifiable<Id>, R : Identifiable<Id>> Flux<T>.extendById(mapper: (T) -> Mono<R>): Mono<List<R>> {
    return this.collectList()
        .flatMap { it.extendById(mapper) }
}

fun <Id, T : Identifiable<Id>, R : Identifiable<Id>> List<T>.extendById(mapper: (T) -> Mono<R>): Mono<List<R>> {
    return Flux.fromIterable(this)
        .flatMap(mapper)
        .collectList()
        .map { result ->
            val map = result.map { it.id to it }.toMap()
            this.map { map.getValue(it.id) }
        }
}
