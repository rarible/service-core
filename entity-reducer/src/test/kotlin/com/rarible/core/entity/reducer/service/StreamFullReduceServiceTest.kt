package com.rarible.core.entity.reducer.service

import com.rarible.core.entity.reducer.service.model.Erc20Balance
import com.rarible.core.entity.reducer.service.model.Erc20BalanceEvent
import com.rarible.core.entity.reducer.service.service.Erc20BalanceEntityIdService
import com.rarible.core.entity.reducer.service.service.Erc20BalanceForwardEventApplyPolicy
import com.rarible.core.entity.reducer.service.service.Erc20BalanceReducer
import com.rarible.core.entity.reducer.service.service.Erc20BalanceService
import com.rarible.core.entity.reducer.service.service.Erc20BalanceTemplateProvider
import com.rarible.core.test.data.randomInt
import com.rarible.core.test.data.randomLong
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class StreamFullReduceServiceTest {
    @Test
    fun `should make full reduce of single entity`() = runBlocking<Unit> {
        val entityService = Erc20BalanceService()
        val task = createStreamReduceService(entityService)

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
    fun `should make full reduce of existed single entity`() = runBlocking<Unit> {
        val entityService = Erc20BalanceService()
        val task = createStreamReduceService(entityService)
        val exists = Erc20Balance(
            id = 28,
            balance = 100,
            revertableEvents = emptyList(),
            version = 100
        )
        entityService.update(exists)

        val entityId = exists.id
        val events = listOf(
            Erc20BalanceEvent(
                block = 1,
                value = 1,
                entityId = entityId
            )
        )
        val updatedEntity = mutableListOf<Erc20Balance>()
        task.reduce(events.asFlow()).collect { entity ->
            assertThat(entity.id).isEqualTo(exists.id)
            assertThat(entity.balance).isEqualTo(1)
            assertThat(entity.revertableEvents).isEqualTo(events)
            assertThat(entity.version).isEqualTo(102)
            // updatedAt hasn't been changed - full reduce should not specify last event for update procedure
            assertThat(entity.updatedAt).isNotEqualTo(exists.updatedAt)
            updatedEntity.add(entity)
        }
        assertThat(updatedEntity).hasSize(1)
    }

    @Test
    fun `should make full reduce of many entities`() = runBlocking<Unit> {
        val entityService = Erc20BalanceService()
        val task = createStreamReduceService(entityService)

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

    @Test
    fun `should return events every throttleSaveToDb`() = runBlocking<Unit> {
        val entityService = Erc20BalanceService()
        val task = createStreamReduceService(entityService, 3)
        val entitiesAndEvents = task.reduceWithEvents(flowOf(
            Erc20BalanceEvent(entityId = 1L, block = 10, value = 100),
            Erc20BalanceEvent(entityId = 2L, block = 11, value = 100),
            Erc20BalanceEvent(entityId = 2L, block = 12, value = 100),
            Erc20BalanceEvent(entityId = 3L, block = 13, value = 100),
            Erc20BalanceEvent(entityId = 3L, block = 14, value = 100),
            Erc20BalanceEvent(entityId = 3L, block = 15, value = 100),
            Erc20BalanceEvent(entityId = 4L, block = 16, value = 100),
            Erc20BalanceEvent(entityId = 4L, block = 17, value = 100),
            Erc20BalanceEvent(entityId = 4L, block = 18, value = 100),
            Erc20BalanceEvent(entityId = 4L, block = 19, value = 100),
        )).toList()

        assertThat(entitiesAndEvents)
            .extracting<Pair<Int, Long>> { (entity, event) -> entity.balance to event.block }
            .containsExactly(
                100 to 10,
                200 to 12,
                300 to 15,
                300 to 18,
                400 to 19
            )
    }

    @Test
    fun `shouldn't make useless update during full reduce of existed single entity`() = runBlocking<Unit> {
        val entityService = Erc20BalanceService()
        val task = createStreamReduceServiceWithComparing(entityService)
        val existed = Erc20Balance(
            id = 28,
            balance = 100,
            revertableEvents = emptyList(),
            version = 0
        )
        entityService.update(existed)

        val entityId = existed.id
        val events = listOf(
            Erc20BalanceEvent(
                block = 1,
                value = 1,
                entityId = entityId
            )
        )
        val updatedEntity = mutableListOf<Erc20Balance>()
        task.reduce(events.asFlow()).collect { entity ->
            assertThat(entity.version).isEqualTo(2)
            updatedEntity.add(entity)
        }

        // second try
        task.reduce(events.asFlow()).collect { entity ->
            assertThat(entity.version).isEqualTo(2)
            updatedEntity.add(entity)
        }
        assertThat(updatedEntity).hasSize(2)
    }

    @Test
    fun `shouldn't make useless update during full reduce of existed many entities`() = runBlocking<Unit> {
        val entityService = Erc20BalanceService()
        val task = createStreamReduceServiceWithComparing(entityService)
        val existed1 = Erc20Balance(
            id = 28,
            balance = 100,
            revertableEvents = emptyList(),
            version = 0
        )
        entityService.update(existed1)
        val existed2 = Erc20Balance(
            id = 29,
            balance = 10,
            revertableEvents = emptyList(),
            version = 0
        )
        entityService.update(existed2)

        val events = listOf(existed1, existed2).map {
            Erc20BalanceEvent(
                block = 1,
                value = 1,
                entityId = it.id
            )
        }
        val updatedEntity = mutableListOf<Erc20Balance>()
        task.reduce(events.asFlow()).collect { entity ->
            assertThat(entity.version).isEqualTo(2)
            updatedEntity.add(entity)
        }

        // second try
        task.reduce(events.asFlow()).collect { entity ->
            assertThat(entity.version).isEqualTo(2)
            updatedEntity.add(entity)
        }
        assertThat(updatedEntity).hasSize(4)
    }

    private fun createStreamReduceService(
        entityService: Erc20BalanceService,
        throttleSaveToDb: Int = 100
    ): StreamFullReduceService<Long, Erc20BalanceEvent, Erc20Balance> {
        val entityIdService = Erc20BalanceEntityIdService()
        val templateProvider = Erc20BalanceTemplateProvider()
        val eventRevertPolicy = Erc20BalanceForwardEventApplyPolicy()
        val reducer = EntityReducer(eventRevertPolicy, Erc20BalanceReducer())

        return StreamFullReduceService(
            entityService,
            entityIdService,
            templateProvider,
            reducer,
            throttleSaveToDb
        )
    }

    private fun createStreamReduceServiceWithComparing(entityService: Erc20BalanceService): StreamFullReduceService<Long, Erc20BalanceEvent, Erc20Balance> {
        val entityIdService = Erc20BalanceEntityIdService()
        val templateProvider = Erc20BalanceTemplateProvider()
        val eventRevertPolicy = Erc20BalanceForwardEventApplyPolicy()
        val reducer = EntityReducer(eventRevertPolicy, Erc20BalanceReducer())

        return object : StreamFullReduceService<Long, Erc20BalanceEvent, Erc20Balance>(
            entityService,
            entityIdService,
            templateProvider,
            reducer,
            throttleSaveToDb = 100
        ) {
            override fun isChanged(current: Erc20Balance?, result: Erc20Balance?): Boolean {
                return current?.balance != result?.balance
            }
        }
    }
}
