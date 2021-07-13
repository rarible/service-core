package com.rarible.core.reduce.service.repository

import com.rarible.core.reduce.repository.DataRepository
import com.rarible.core.reduce.service.model.AccountBalance

class ReduceDataRepository(
    private val delegate: AccountBalanceRepository
) : DataRepository<AccountBalance> {

    override suspend fun saveReduceResult(data: AccountBalance) {
        val currentBalance = delegate.get(data.id)
        delegate.save(currentBalance?.withBalance(data.balance) ?: data)
    }
}