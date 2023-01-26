package com.rarible.core.entity.reducer.service

import com.rarible.core.common.nowMillis
import com.rarible.core.entity.reducer.service.model.Erc20Balance
import com.rarible.core.entity.reducer.service.model.Erc20BalanceEvent
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class RevertedEntityReducerTest {
    private val eventRevertPolicy = mockk<EventApplyPolicy<Erc20BalanceEvent>>()
    private val reversedReducer = mockk<Reducer<Erc20BalanceEvent, Erc20Balance>>()
    private val revertedEntityReducer = RevertedEntityReducer(eventRevertPolicy, reversedReducer)

    @Test
    fun `should apply event`() = runBlocking<Unit> {
        val date1 = nowMillis().minusSeconds(1)
        val date2 = nowMillis()
        val events = listOf(
            Erc20BalanceEvent(block = 1, date = date1),
            Erc20BalanceEvent(block = 2, date = date2)
        )
        val entity = Erc20Balance(id = 0, balance = 1, revertableEvents = events)
        val expectedEvent = events[0]
        val revertEvent = events[1]

        every { eventRevertPolicy.wasApplied(events, revertEvent) } returns true
        every { eventRevertPolicy.reduce(events, revertEvent) } returns events - revertEvent
        coEvery { reversedReducer.reduce(entity, revertEvent) } returns entity

        val updatedEntity = revertedEntityReducer.reduce(entity, revertEvent)
        assertThat(updatedEntity.revertableEvents).isEqualTo(listOf(expectedEvent))

        coVerify { reversedReducer.reduce(entity, revertEvent) }
    }

    @Test
    fun `should not apply event`() = runBlocking<Unit> {
        val events = listOf(Erc20BalanceEvent(block = 1), Erc20BalanceEvent(block = 2))
        val entity = Erc20Balance(id = 0, balance = 1, revertableEvents = events)
        val revertEvent = Erc20BalanceEvent(block = 3)

        every { eventRevertPolicy.wasApplied(events, revertEvent) } returns false
        coEvery { reversedReducer.reduce(entity, revertEvent) } returns entity

        val updatedEntity = revertedEntityReducer.reduce(entity, revertEvent)
        Assertions.assertThat(updatedEntity.revertableEvents).isEqualTo(events)

        coVerify(exactly = 0) { reversedReducer.reduce(any(), any()) }
    }
}
