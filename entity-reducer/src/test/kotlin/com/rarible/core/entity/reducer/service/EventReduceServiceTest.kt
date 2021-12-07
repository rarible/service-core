package com.rarible.core.entity.reducer.service

import com.rarible.core.entity.reducer.service.model.EntityEvent
import com.rarible.core.entity.reducer.service.model.TestEntity
import com.rarible.core.entity.reducer.service.service.TestEntityReducer
import io.mockk.*
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

internal class EventReduceServiceTest {
    private val entityService = mockk<EntityService<Long, TestEntity>>()
    private val entityEventService = mockk<EntityEventService<EntityEvent, Long>>()
    private val templateProvider = mockk<EntityTemplateProvider<Long, TestEntity>>()
    private val reducer = TestEntityReducer()

    private val eventReduceService = EventReduceService(
        entityService,
        entityEventService,
        templateProvider,
        reducer
    )

    @Test
    fun `should reduce all events`() = runBlocking<Unit> {
        val entityId1 = 1L
        val events1 = listOf(
            EntityEvent(
                block = 1,
                intValue = 1,
                entityId = entityId1
            ),
            EntityEvent(
                block = 1,
                intValue = 2,
                entityId = entityId1
            )
        )
        val expectedEntity1 = TestEntity(
            events = emptyList(),
            intValue = 3,
            id = entityId1
        )

        val entityId2 = 2L
        val events2 = listOf(
            EntityEvent(
                block = 1,
                intValue = 1,
                entityId = entityId2
            ),
            EntityEvent(
                block = 1,
                intValue = 2,
                entityId = entityId2
            )
        )
        val expectedEntity2 = TestEntity(
            events = emptyList(),
            intValue = 3,
            id = entityId2
        )

        coEvery { entityService.get(entityId1) } returns null
        coEvery { entityService.get(entityId2) } returns null
        coEvery { entityService.update(expectedEntity1) } returns expectedEntity1
        coEvery { entityService.update(expectedEntity2) } returns expectedEntity2

        every { templateProvider.getEntityTemplate(entityId1) } returns TestEntity(
            events = emptyList(),
            intValue = 0,
            id = entityId1
        )
        every { templateProvider.getEntityTemplate(entityId2) } returns TestEntity(
            events = emptyList(),
            intValue = 0,
            id = entityId2
        )
        every { entityEventService.getEntityId(any()) } answers {
            (args.single() as EntityEvent).entityId
        }
        eventReduceService.reduceAll((events1 + events2))

        coVerify(exactly = 1) { entityService.update(expectedEntity1) }
        coVerify(exactly = 1) { entityService.update(expectedEntity2) }
    }
}
