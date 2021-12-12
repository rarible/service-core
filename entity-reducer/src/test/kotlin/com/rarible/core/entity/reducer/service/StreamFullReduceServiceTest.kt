package com.rarible.core.entity.reducer.service

import com.rarible.core.entity.reducer.service.model.Erc20BalanceEvent
import com.rarible.core.entity.reducer.service.model.Erc20Balance
import com.rarible.core.entity.reducer.service.service.*
import com.rarible.core.test.data.randomInt
import com.rarible.core.test.data.randomLong
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class StreamFullReduceServiceTest {
    @Test
    fun `should make full reduce of single entity`() = runBlocking<Unit> {
        val entityService = Erc20BalanceService()
        val task = createTaskService(entityService)

        val entityId = randomLong()
        val events = listOf(
            Erc20BalanceEvent(
                block = 1,
                value = 1,
                entityId = entityId
            ),
            Erc20BalanceEvent(
                block = 2,
                value = 2,
                entityId = entityId
            ),
            Erc20BalanceEvent(
                block = 3,
                value = 3,
                entityId = entityId
            )
        )
        val updatedEntity = mutableListOf<Erc20Balance>()

        task.reduce(events.asFlow()).collect { entity ->
            assertThat(entity.id).isEqualTo(entityId)
            assertThat(entity.balance).isEqualTo(6)
            assertThat(entity.revertableEvents).isEqualTo(events)
            updatedEntity.add(entity)
        }
        assertThat(updatedEntity).hasSize(1)
        assertThat(entityService.getUpdateCount()).isEqualTo(1)
    }


    @Test
    fun `should make full reduce of many entities`() = runBlocking<Unit> {
        val entityService = Erc20BalanceService()
        val task = createTaskService(entityService)

        val events = (1..100).map {
            val entityId = randomLong()
            val values = (1..(10..20).random()).map { randomInt() }

            var block = 0L
            val events = values.map { value ->
                Erc20BalanceEvent(
                    block = block++,
                    value = value,
                    entityId = entityId
                )
            }
            entityId to events
        }.toMap()

        val updatedEntity = mutableListOf<Erc20Balance>()

        task.reduce(events.values.flatten().asFlow()).collect { entity ->
            val expectedEvents = events[entity.id]
            val expectedBalance = expectedEvents?.sumOf { it.value }

            assertThat(entity.balance).isEqualTo(expectedBalance)
            assertThat(entity.revertableEvents).isEqualTo(expectedEvents)
            updatedEntity.add(entity)
        }
        assertThat(updatedEntity.map { it.id }).containsExactly(*events.keys.toTypedArray())
        assertThat(entityService.getUpdateCount()).isEqualTo(events.keys.size.toLong())
    }

    private fun createTaskService(entityService: Erc20BalanceService): StreamFullReduceService<Long, Erc20BalanceEvent, Erc20Balance> {
        val entityEventService = Erc20BalanceEntityEventService()
        val templateProvider = Erc20BalanceTemplateProvider()
        val eventRevertPolicy = Erc20BalanceForwardEventRevertPolicy()
        val reducer = EntityReducer(eventRevertPolicy, Erc20BalanceReducer())

        return StreamFullReduceService(
            entityService,
            entityEventService,
            templateProvider,
            reducer
        )
    }
}
