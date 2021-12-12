package com.rarible.core.entity.reducer.chain

import com.rarible.core.entity.reducer.service.Reducer
import com.rarible.core.entity.reducer.service.model.Erc20Balance
import com.rarible.core.entity.reducer.service.model.Erc20BalanceEvent
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class ReducersChainTest {
    private val reduce1 = object : Reducer<Erc20BalanceEvent, Erc20Balance> {
        override suspend fun reduce(entity: Erc20Balance, event: Erc20BalanceEvent): Erc20Balance {
            return entity.copy(balance = entity.balance * event.value )
        }
    }
    private val reduce2 = object : Reducer<Erc20BalanceEvent, Erc20Balance> {
        override suspend fun reduce(entity: Erc20Balance, event: Erc20BalanceEvent): Erc20Balance {
            return entity.copy(balance = entity.balance + event.value )
        }
    }
    private val chain = ReducersChain(listOf(reduce1, reduce2))

    @Test
    fun testChain() = runBlocking<Unit> {
        val start = Erc20Balance(
            id = 0,
            balance = 2,
            revertableEvents = emptyList()
        )
        val event = Erc20BalanceEvent(
            block = 1,
            value = 2,
            entityId = 1
        )

        val reduced = chain.reduce(start, event)
        assertThat(reduced.balance).isEqualTo(6)
    }
}
