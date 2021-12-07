package com.rarible.core.entity.reducer.service

import com.rarible.core.entity.reducer.exception.ReduceException
import com.rarible.core.entity.reducer.service.model.EntityEvent
import com.rarible.core.entity.reducer.service.model.TestEntity
import io.mockk.*
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

internal class RevertableEntityReversedReducerTest {
    private val reversedReducer = mockk<ReversedReducer<EntityEvent, TestEntity>>()
    private val revertableEntityReducer = RevertableEntityReversedReducer(reversedReducer)

    @Test
    fun `should apply event`() = runBlocking<Unit> {
        val events = listOf(EntityEvent(block = 1), EntityEvent(block = 2))
        val entity = TestEntity(events = events)
        val revertEvent = EntityEvent(block = 2)

        coEvery { reversedReducer.reduce(entity, revertEvent) } returns entity

        val updatedEntity = revertableEntityReducer.reduce(entity, revertEvent)
        Assertions.assertThat(updatedEntity.events).isEqualTo(listOf(EntityEvent(block = 1)))
    }

    @Test
    fun `should not apply event`() = runBlocking<Unit> {
        val events = listOf(EntityEvent(block = 1), EntityEvent(block = 2))
        val entity = TestEntity(events = events)
        val revertEvent = EntityEvent(block = 3)

        coEvery { reversedReducer.reduce(entity, revertEvent) } returns entity

        val updatedEntity = revertableEntityReducer.reduce(entity, revertEvent)
        Assertions.assertThat(updatedEntity.events).isEqualTo(events)
    }

    @Test
    fun `should throw exception as we try to revert too old event`() = runBlocking<Unit> {
        val events = listOf(EntityEvent(block = 1), EntityEvent(block = 2))
        val entity = TestEntity(events = events)
        val revertEvent = EntityEvent(block = 0)

        assertThrows<ReduceException> {
            runBlocking {
                revertableEntityReducer.reduce(entity, revertEvent)
            }
        }
    }

    @Test
    fun `should not apply event if empty list`() = runBlocking<Unit> {
        val entity = TestEntity(events = emptyList())
        val revertEvent = EntityEvent(block = 1)

        revertableEntityReducer.reduce(entity, revertEvent)
        coEvery { reversedReducer.reduce(entity, revertEvent) } returns entity

        val updatedEntity = revertableEntityReducer.reduce(entity, revertEvent)
        Assertions.assertThat(updatedEntity.events).isEqualTo(emptyList<EntityEvent>())
    }
}
