package com.rarible.core.reduce.service.reducer

import com.rarible.core.reduce.service.Reducer
import com.rarible.core.reduce.service.model.*
import com.rarible.core.reduce.service.repository.AccountBalanceRepository
import java.math.BigInteger

class AccountBalanceReducer(
    private val accountBalanceRepository: AccountBalanceRepository
) : Reducer<AccountReduceEvent, AccountReduceSnapshot, Long, AccountBalance, AccountId> {

    override fun getDataKeyFromEvent(event: AccountReduceEvent): AccountId {
        return AccountId(bank = event.event.bank, owner = event.event.owner)
    }

    override fun getInitialData(key: AccountId): AccountReduceSnapshot {
        val accountBalance = AccountBalance(
            bank = key.bank,
            owner = key.owner,
            balance = BigInteger.ZERO
        )
        return AccountReduceSnapshot(accountBalance.id, accountBalance, 0)
    }

    override suspend fun reduce(
        initial: AccountReduceSnapshot,
        event: AccountReduceEvent
    ): AccountReduceSnapshot {
        val initialBalance = initial.data.balance
        val accountBalance = accountBalanceRepository.get(initial.data.id) ?: initial.data

        val reducedBalance = when (event.event) {
            is AccountIncomeTransfer -> initial.data.withBalance(initialBalance + event.event.value)
            is AccountOutcomeTransfer -> initial.data.withBalance(initialBalance - event.event.value)
        }
        return AccountReduceSnapshot(
            id = accountBalance.id,
            data = reducedBalance,
            mark = event.mark
        )
    }
}