package com.rarible.core.entity.reducer.service.service

import com.rarible.core.entity.reducer.service.Reducer
import com.rarible.core.entity.reducer.service.model.Erc20Balance
import com.rarible.core.entity.reducer.service.model.Erc20BalanceEvent

class Erc20BalanceReducer : Reducer<Erc20BalanceEvent, Erc20Balance> {
    override suspend fun reduce(entity: Erc20Balance, event: Erc20BalanceEvent): Erc20Balance {
        return entity.copy(balance = entity.balance + event.value)
    }
}

