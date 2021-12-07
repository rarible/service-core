package com.rarible.core.entity.reducer.service

import com.rarible.core.entity.reducer.service.model.EntityEvent
import com.rarible.core.entity.reducer.service.model.TestEntity
import com.rarible.core.entity.reducer.service.service.TestEntityReducer
import io.mockk.*
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class TaskReduceServiceTest {
    private val entityService = mockk<EntityService<Long, TestEntity>>()
    private val entityEventService = mockk<EntityEventService<EntityEvent, Long>>()
    private val templateProvider = mockk<EntityTemplateProvider<Long, TestEntity>>()
    private val reducer = TestEntityReducer()

    private val taskReduceService = TaskReduceService(
        entityService,
        entityEventService,
        templateProvider,
        reducer
    )

    @Test
    fun `should make full reduce of single entity`() = runBlocking<Unit> {
        val entityId = 1L
        val events = listOf(
            EntityEvent(
                block = 1,
                intValue = 1,
                entityId = entityId
            ),
            EntityEvent(
                block = 1,
                intValue = 2,
                entityId = entityId
            ),
            EntityEvent(
                block = 1,
                intValue = 3,
                entityId = entityId
            )
        )
        every { entityEventService.getEntityId(any()) } answers {
            (args.single() as EntityEvent).entityId
        }
        every { templateProvider.getEntityTemplate(entityId) } returns TestEntity(
            events = emptyList(),
            id = entityId
        )
        coEvery { entityService.update(any()) } answers {
            args.first() as TestEntity
        }
        val updatedEntity = mutableListOf<TestEntity>()

        taskReduceService.reduce(events.asFlow()).collect { entity ->
            assertThat(entity.intValue).isEqualTo(6)

            updatedEntity.add(entity)
        }
        assertThat(updatedEntity).hasSize(1)
        coVerify(exactly = 1) { entityService.update(updatedEntity.single()) }
        coVerify(exactly = 1) { templateProvider.getEntityTemplate(entityId) }
    }


    @Test
    fun `should make full reduce of many entities`() = runBlocking<Unit> {
        val entityId1 = 1L
        val events1 = listOf(
            EntityEvent(
                block = 1,
                intValue = 1,
                entityId = entityId1
            )
        )

        val entityId2 = 2L
        val events2 = listOf(
            EntityEvent(
                block = 1,
                intValue = 1,
                entityId = entityId2
            )
        )

        every { entityEventService.getEntityId(any()) } answers {
            (args.single() as EntityEvent).entityId
        }

        every { templateProvider.getEntityTemplate(any()) } answers {
            TestEntity(
                events = emptyList(),
                id = args.single() as Long
            )
        }
        coEvery { entityService.update(any()) } answers {
            args.first() as TestEntity
        }

        val updatedEntity = mutableListOf<TestEntity>()

        taskReduceService.reduce((events1 + events2).asFlow()).collect { entity ->
            assertThat(entity.intValue).isEqualTo(1)
            updatedEntity.add(entity)
        }
        assertThat(updatedEntity).hasSize(2)
        coVerify(exactly = 1) { entityService.update(updatedEntity[0]) }
        coVerify(exactly = 1) { entityService.update(updatedEntity[1]) }
    }
}
