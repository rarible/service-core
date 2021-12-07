package com.rarible.core.entity.reducer.service

import com.rarible.core.entity.reducer.service.model.EntityEvent
import com.rarible.core.entity.reducer.service.model.TestEntity
import io.mockk.*
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class RevertableEntityReducerTest {
    private val eventRevertService = mockk<EventRevertService<EntityEvent>>()
    private val reducer = mockk<Reducer<EntityEvent, TestEntity>>()
    private val revertableEntityReducer = RevertableEntityReducer(eventRevertService, reducer)

    @Test
    fun `should apply event`() = runBlocking<Unit> {
        val events = listOf(EntityEvent(block = 1), EntityEvent(block = 2))
        val entity = TestEntity(events = events)
        val newEvent = EntityEvent(block = 3)

        every { eventRevertService.canBeReverted(newEvent, events[0]) } returns true
        every { eventRevertService.canBeReverted(newEvent, events[1]) } returns true
        every { eventRevertService.canBeReverted(newEvent, newEvent) } returns true
        coEvery { reducer.reduce(entity, newEvent) } returns entity

        val updatedEntity = revertableEntityReducer.reduce(entity, newEvent)
        assertThat(updatedEntity.events).isEqualTo(events + newEvent)
    }

    @Test
    fun `should not apply event`() = runBlocking<Unit> {
        val events = listOf(EntityEvent(block = 1), EntityEvent(block = 2))
        val entity = TestEntity(events = events)
        val newEvent = EntityEvent(block = 2)

        val updatedEntity = revertableEntityReducer.reduce(entity, newEvent)
        assertThat(updatedEntity.events).isEqualTo(events)

        verify(exactly = 0) { eventRevertService.canBeReverted(any(), any()) }
        coVerify(exactly = 0) { reducer.reduce(any(), any()) }
    }
}
