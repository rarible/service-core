package com.rarible.core.entity.reducer.service

import com.rarible.core.entity.reducer.service.model.Erc20BalanceEvent
import com.rarible.core.entity.reducer.service.model.Erc20Balance
import io.mockk.*
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test

internal class EntityReversedReducerTest {
    private val eventRevertPolicy = mockk<EventApplyPolicy<Erc20BalanceEvent>>()
    private val reversedReducer = mockk<Reducer<Erc20BalanceEvent, Erc20Balance>>()
    private val revertableEntityReducer = RevertedEntityReducer(eventRevertPolicy, reversedReducer)

    @Test
    fun `should apply event`() = runBlocking<Unit> {
        val events = listOf(Erc20BalanceEvent(block = 1), Erc20BalanceEvent(block = 2))
        val entity = Erc20Balance(id = 0, balance = 1, revertableEvents = events)
        val revertEvent = Erc20BalanceEvent(block = 2)

        every { eventRevertPolicy.wasApplied(events, revertEvent) } returns true
        every { eventRevertPolicy.reduce(events, revertEvent) } returns events - revertEvent
        coEvery { reversedReducer.reduce(entity, revertEvent) } returns entity

        val updatedEntity = revertableEntityReducer.reduce(entity, revertEvent)
        Assertions.assertThat(updatedEntity.revertableEvents).isEqualTo(listOf(Erc20BalanceEvent(block = 1)))

        coVerify { reversedReducer.reduce(entity, revertEvent) }
    }

    @Test
    fun `should not apply event`() = runBlocking<Unit> {
        val events = listOf(Erc20BalanceEvent(block = 1), Erc20BalanceEvent(block = 2))
        val entity = Erc20Balance(id = 0, balance = 1, revertableEvents = events)
        val revertEvent = Erc20BalanceEvent(block = 3)

        every { eventRevertPolicy.wasApplied(events, revertEvent) } returns false
        coEvery { reversedReducer.reduce(entity, revertEvent) } returns entity

        val updatedEntity = revertableEntityReducer.reduce(entity, revertEvent)
        Assertions.assertThat(updatedEntity.revertableEvents).isEqualTo(events)

        coVerify(exactly = 0) { reversedReducer.reduce(any(), any()) }
    }
}
