package com.rarible.core.common

import org.springframework.dao.DuplicateKeyException
import org.springframework.dao.OptimisticLockingFailureException
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference

suspend fun <T> optimisticLock(attempts: Long = 5, update: suspend () -> T): T {
    val retry = AtomicLong(0)
    val last = AtomicReference<Throwable?>(null)

    do {
        try {
            return update()
        } catch (ex: OptimisticLockingFailureException) {
            ex
        } catch (ex: DuplicateKeyException) {
            ex
        }.let { last.set(it) }
    } while (retry.incrementAndGet() < attempts)

    throw last.get() ?: error("Last error unexpectedly null")
}

suspend fun <T> optimisticLock(
    attempts: Long = 5,
    latest: T,
    getLatest: suspend (T) -> T,
    update: suspend (T) -> T
): T {
    val retry = AtomicLong(0)
    val last = AtomicReference<Throwable?>(null)
    val latestVersion = AtomicReference(latest)

    do {
        val version = latestVersion.get()
        try {
            return update(version)
        } catch (ex: OptimisticLockingFailureException) {
            ex
        } catch (ex: DuplicateKeyException) {
            ex
        }.let { last.set(it) }

        latestVersion.set(getLatest(version))
    } while (retry.incrementAndGet() < attempts)

    throw last.get() ?: error("Last error unexpectedly null")
}
