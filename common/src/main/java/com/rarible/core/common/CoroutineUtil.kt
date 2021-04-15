package com.rarible.core.common

import org.springframework.dao.DuplicateKeyException
import org.springframework.dao.OptimisticLockingFailureException

suspend fun <T> optimisticLock(attempts: Long = 5, update: suspend () -> T): T {
    var retry = 0
    var last: Throwable

    do {
        last = try {
            return update()
        } catch (ex: OptimisticLockingFailureException) {
            ex
        } catch (ex: DuplicateKeyException) {
            ex
        }
    } while (++retry < attempts)

    throw last
}