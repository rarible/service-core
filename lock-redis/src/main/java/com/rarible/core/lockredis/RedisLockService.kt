package com.rarible.core.lockredis

import com.rarible.core.common.toOptional
import com.rarible.core.lock.LockService
import io.lettuce.core.SetArgs
import io.lettuce.core.api.reactive.RedisReactiveCommands
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.switchIfEmpty
import reactor.util.retry.Retry
import java.time.Duration
import kotlin.random.Random

@Service
class RedisLockService(
    private val client: RedisReactiveCommands<String, String>
) : LockService {

    private val backoff = Retry
        .backoff(Long.MAX_VALUE, Duration.ofMillis(300))
        .maxBackoff(Duration.ofSeconds(1))

    override fun <T> synchronize(name: String, expiresMs: Int, op: Mono<T>): Mono<T> {
        return acquireLock(name, expiresMs)
            .flatMap { id ->
                op.flatMap { unlock(name, id).thenReturn(it) }
                    .switchIfEmpty { unlock(name, id).then(Mono.empty()) }
                    .onErrorResume { unlock(name, id).then(Mono.error(it)) }
            }
    }

    private fun acquireLock(key: String, expiresMs: Int): Mono<Long> {
        val id = Random.nextLong(0, Long.MAX_VALUE)
        return tryLock(key, id, expiresMs)
            .thenReturn(id)
            .retryWhen(backoff)
    }

    @Suppress("RemoveExplicitTypeArguments")
    private fun tryLock(key: String, id: Long, expiresMs: Int): Mono<Void> {
        return client.set(key, id.toString(), SetArgs().nx().px(expiresMs.toLong()))
            .toOptional()
            .flatMap<Void> {
                if (it.isPresent && it.get() == "OK")
                    Mono.empty()
                else
                    Mono.error(IllegalStateException("lock $key is not available"))
            }
    }

    private fun unlock(key: String, id: Long): Mono<Void> {
        return client.get(key)
            .flatMap {
                if (it == id.toString()) {
                    client.del(key).then()
                } else {
                    Mono.empty()
                }
            }
    }
}
