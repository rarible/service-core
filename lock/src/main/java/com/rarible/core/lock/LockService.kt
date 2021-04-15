package com.rarible.core.lock

import reactor.core.publisher.Mono

interface LockService {
    fun <T> synchronize(name: String, expiresMs: Int, op: Mono<T>): Mono<T>
}