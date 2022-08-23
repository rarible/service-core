package com.rarible.core.entity.reducer.service.service

import com.rarible.core.entity.reducer.service.EntityService
import com.rarible.core.entity.reducer.service.model.Erc20Balance
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

class Erc20BalanceService : EntityService<Long, Erc20Balance> {
    private val updateCount = AtomicLong(0)

    private val storage = ConcurrentHashMap<Long, Erc20Balance>()

    fun getUpdateCount(): Long = updateCount.get()

    override suspend fun get(id: Long): Erc20Balance? {
        return storage[id]
    }

    override suspend fun update(entity: Erc20Balance): Erc20Balance {
        storage[entity.id] = entity.copy(version = (entity.version ?: 0L) + 1L)
        updateCount.getAndIncrement()
        return storage[entity.id] ?: error("Unexpected state")
    }
}
