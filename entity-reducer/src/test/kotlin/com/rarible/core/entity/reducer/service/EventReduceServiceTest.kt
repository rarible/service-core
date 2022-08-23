package com.rarible.core.entity.reducer.service

import com.rarible.core.entity.reducer.service.model.Erc20BalanceEvent
import com.rarible.core.entity.reducer.service.model.Erc20Balance
import com.rarible.core.entity.reducer.service.service.Erc20BalanceEntityIdService
import com.rarible.core.entity.reducer.service.service.Erc20BalanceReducer
import com.rarible.core.entity.reducer.service.service.Erc20BalanceService
import com.rarible.core.entity.reducer.service.service.Erc20BalanceTemplateProvider
import com.rarible.core.test.data.randomLong
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class EventReduceServiceTest {
    private val entityService = Erc20BalanceService()
    private val entityIdService = Erc20BalanceEntityIdService()
    private val templateProvider = Erc20BalanceTemplateProvider()
    private val reducer = Erc20BalanceReducer()

    private val eventReduceService = EventReduceService(
        entityService,
        entityIdService,
        templateProvider,
        reducer
    )

    @Test
    fun `should reduce all events`() = runBlocking<Unit> {
        val entityId1 = randomLong()
        val events1 = listOf(
            Erc20BalanceEvent(
                block = 1,
                value = 1,
                entityId = entityId1
            ),
            Erc20BalanceEvent(
                block = 1,
                value = 2,
                entityId = entityId1
            )
        )
        val expectedEntity1 = Erc20Balance(
            revertableEvents = emptyList(),
            balance = 3,
            id = entityId1,
            version = 1
        )

        val entityId2 = randomLong()
        val events2 = listOf(
            Erc20BalanceEvent(
                block = 1,
                value = 1,
                entityId = entityId2
            ),
            Erc20BalanceEvent(
                block = 1,
                value = 2,
                entityId = entityId2
            )
        )
        val expectedEntity2 = Erc20Balance(
            revertableEvents = emptyList(),
            balance = 3,
            id = entityId2,
            version = 1
        )

        eventReduceService.reduceAll((events1 + events2))

        val reducedEntity1 = entityService.get(entityId1)
        val reducedEntity2 = entityService.get(entityId2)

        assertThat(reducedEntity1).isEqualTo(expectedEntity1)
        assertThat(reducedEntity2).isEqualTo(expectedEntity2)
    }
}
