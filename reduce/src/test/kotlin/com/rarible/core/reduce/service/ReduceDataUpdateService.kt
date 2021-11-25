package com.rarible.core.reduce.service

import com.rarible.core.reduce.service.model.AccountBalance
import com.rarible.core.reduce.service.repository.AccountBalanceRepository

class ReduceDataUpdateService(
    private val delegate: AccountBalanceRepository
) : UpdateService<AccountBalance> {

    override suspend fun update(data: AccountBalance) {
        val currentBalance = delegate.get(data.id)
        delegate.save(currentBalance?.withBalance(data.balance) ?: data)
    }
}
