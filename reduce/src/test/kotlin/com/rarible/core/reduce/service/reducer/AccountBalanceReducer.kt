package com.rarible.core.reduce.service.reducer

import com.rarible.core.reduce.service.Reducer
import com.rarible.core.reduce.service.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.fold
import java.math.BigInteger

class AccountBalanceReducer : Reducer<AccountReduceEvent, AccountReduceSnapshot, Long, AccountBalance, AccountId> {
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
        events: Flow<AccountReduceEvent>
    ): AccountReduceSnapshot {
        val accountBalance = initial.data
        val initialState = BalanceState(accountBalance.balance, initial.mark)

        val (balance, mark) = events.fold(initialState) { balance, history ->
            val value = history.event.value
            val mark = history.mark

            when (history.event) {
                is AccountIncomeTransfer -> balance.plus(value, mark)
                is AccountOutcomeTransfer -> balance.minus(value, mark)
            }
        }
        return AccountReduceSnapshot(
            id = accountBalance.id,
            data = accountBalance.withBalance(balance),
            mark = mark
        )
    }

    private data class BalanceState(
        val balance: BigInteger,
        val mark: Long
    ) {
        fun minus(balance: BigInteger, mark: Long): BalanceState {
            return copy(balance = this.balance - balance, mark = mark)
        }

        fun plus(balance: BigInteger, mark: Long): BalanceState {
            return copy(balance = this.balance + balance, mark = mark)
        }
    }
}