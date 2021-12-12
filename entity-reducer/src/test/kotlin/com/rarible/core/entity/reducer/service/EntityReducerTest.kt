package com.rarible.core.entity.reducer.service

import com.rarible.core.entity.reducer.service.model.Erc20BalanceEvent
import com.rarible.core.entity.reducer.service.model.Erc20Balance
import io.mockk.*
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class EntityReducerTest {
    private val eventRevertPolicy = mockk<EventApplyPolicy<Erc20BalanceEvent>>()
    private val reducer = mockk<Reducer<Erc20BalanceEvent, Erc20Balance>>()
    private val entityReducer = EntityReducer(eventRevertPolicy, reducer)

    @Test
    fun `should apply event`() = runBlocking<Unit> {
        val events = listOf(Erc20BalanceEvent(block = 1), Erc20BalanceEvent(block = 2))
        val entity = Erc20Balance(id = 0, balance = 1, revertableEvents = events)
        val newEvent = Erc20BalanceEvent(block = 3)

        coEvery { eventRevertPolicy.reduce(events, newEvent) } returns events + newEvent
        every { eventRevertPolicy.wasApplied(events, newEvent) } returns false
        coEvery { reducer.reduce(entity, newEvent) } returns entity

        val updatedEntity = entityReducer.reduce(entity, newEvent)
        assertThat(updatedEntity.revertableEvents).isEqualTo(events + newEvent)
    }

    @Test
    fun `should not apply event`() = runBlocking<Unit> {
        val events = listOf(Erc20BalanceEvent(block = 1), Erc20BalanceEvent(block = 2))
        val entity = Erc20Balance(id = 0, balance = 1, revertableEvents = events)
        val newEvent = Erc20BalanceEvent(block = 2)

        coEvery { eventRevertPolicy.wasApplied(events, newEvent) } returns true

        val updatedEntity = entityReducer.reduce(entity, newEvent)
        assertThat(updatedEntity.revertableEvents).isEqualTo(events)

        coVerify(exactly = 0) { eventRevertPolicy.reduce(any(), any()) }
        coVerify(exactly = 0) { reducer.reduce(any(), any()) }
    }
}
