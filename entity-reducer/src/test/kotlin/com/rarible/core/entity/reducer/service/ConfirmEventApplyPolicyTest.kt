package com.rarible.core.entity.reducer.service

import com.rarible.core.entity.reducer.service.model.ItemEvent
import com.rarible.core.entity.reducer.service.model.createRandomItemEvent
import com.rarible.protocol.nft.core.service.policy.ConfirmEventApplyPolicy
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class ConfirmEventApplyPolicyTest {

    private val policy = ConfirmEventApplyPolicy<ItemEvent>(10, { it.status }, { it.blockNumber })

    @Test
    fun `should say false on event if not any confirms events in list`() {
        val events = notConfirmStatuses().map { createRandomItemEvent().copy(status = it) }
        val event = createRandomItemEvent().copy(status = LogStatus.CONFIRMED)
        assertThat(policy.wasApplied(events, event)).isFalse
    }

    @Test
    fun `should say false on event if confirm event is latest`() {
        val events = (1L..20).map { blockNumber ->
            createRandomItemEvent().copy(
                status = LogStatus.CONFIRMED,
                blockNumber = blockNumber
            )
        } + notConfirmStatuses().map { createRandomItemEvent().copy(status = it) }

        val event = createRandomItemEvent().copy(
            status = LogStatus.CONFIRMED,
            blockNumber = 21
        )
        assertThat(policy.wasApplied(events, event)).isFalse()
    }

    @Test
    fun `should say true on event if confirm event is from past`() {
        val events = (10L..20).map { blockNumber ->
            createRandomItemEvent().copy(
                status = LogStatus.CONFIRMED,
                blockNumber = blockNumber
            )
        } + notConfirmStatuses().map { createRandomItemEvent().copy(status = it) }

        val event1 = createRandomItemEvent().copy(
            status = LogStatus.CONFIRMED,
            blockNumber = 9
        )
        val event2 = createRandomItemEvent().copy(
            status = LogStatus.CONFIRMED,
            blockNumber = 1
        )
        assertThat(policy.wasApplied(events, event1)).isTrue()
        assertThat(policy.wasApplied(events, event2)).isTrue()
    }

    @Test
    fun `should add latest event to list`() {
        val events = (1L..5).map { blockNumber ->
            createRandomItemEvent().copy(
                status = LogStatus.CONFIRMED,
                blockNumber = blockNumber
            )
        } + notConfirmStatuses().map { createRandomItemEvent().copy(status = it) }

        val event = createRandomItemEvent().copy(
            status = LogStatus.CONFIRMED,
            blockNumber = 6
        )
        val newEvents = policy.reduce(events, event)
        assertThat(newEvents).isEqualTo(newEvents)
    }

    @Test
    fun `should add latest event to list and remove not revertable events`() {
        val notRevertedEvent1 = createRandomItemEvent().copy(
            status = LogStatus.CONFIRMED,
            blockNumber = 1
        )
        val notRevertedEvent2 = createRandomItemEvent().copy(
            status = LogStatus.CONFIRMED,
            blockNumber = 2
        )
        val notRevertedEvent3 = createRandomItemEvent().copy(
            status = LogStatus.CONFIRMED,
            blockNumber = 3
        )
        val notConfirmEvent1 = createRandomItemEvent().copy(status = LogStatus.PENDING)
        val notConfirmEvent2 = createRandomItemEvent().copy(status = LogStatus.PENDING)
        val events = listOf(notRevertedEvent1, notRevertedEvent2, notRevertedEvent3, notConfirmEvent1, notConfirmEvent2)

        val confirmEvent = createRandomItemEvent().copy(
            status = LogStatus.CONFIRMED,
            blockNumber = 20
        )
        val newEvents = policy.reduce(events, confirmEvent)
        assertThat(newEvents).isEqualTo(listOf(notRevertedEvent3, notConfirmEvent1, notConfirmEvent2, confirmEvent))
    }

    private fun notConfirmStatuses(): List<LogStatus> {
        return LogStatus.values().filter { it != LogStatus.CONFIRMED }
    }
}
