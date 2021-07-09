package com.rarible.core.reduce.service.reducer

import com.rarible.core.reduce.service.Reducer
import com.rarible.core.reduce.service.model.AccountBalance
import com.rarible.core.reduce.service.model.AccountId
import com.rarible.core.reduce.service.model.AccountReduceEvent
import com.rarible.core.reduce.service.model.AccountReduceSnapshot
import kotlinx.coroutines.flow.Flow

class AccountBalanceReducer : Reducer<AccountReduceEvent, AccountReduceSnapshot, Long, AccountBalance, AccountId> {
    override fun getDataKeyFromEvent(event: AccountReduceEvent): AccountId {
        TODO("Not yet implemented")
    }

    override fun getInitialData(key: AccountId): AccountBalance {
        TODO("Not yet implemented")
    }

    override suspend fun reduce(initial: AccountBalance, events: Flow<AccountReduceEvent>): AccountReduceSnapshot {
        TODO("Not yet implemented")
    }
}