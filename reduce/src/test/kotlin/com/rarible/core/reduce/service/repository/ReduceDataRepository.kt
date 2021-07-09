package com.rarible.core.reduce.service.repository

import com.rarible.core.reduce.repository.DataRepository
import com.rarible.core.reduce.service.model.AccountBalance

class ReduceDataRepository(
    private val delegate: AccountBalanceRepository
) : DataRepository<AccountBalance> {

    override suspend fun save(data: AccountBalance) {
        delegate.save(data)
    }
}