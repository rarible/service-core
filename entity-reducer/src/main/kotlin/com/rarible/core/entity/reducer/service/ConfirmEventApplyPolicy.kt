package com.rarible.protocol.nft.core.service.policy

import com.rarible.core.entity.reducer.service.EventApplyPolicy
import com.rarible.core.entity.reducer.service.LogStatus

open class ConfirmEventApplyPolicy<T : Comparable<T>>(
    private val confirmationBlocks: Int,
    private val status: (T) -> LogStatus,
    private val blockNumber: (T) -> Long?
) : EventApplyPolicy<T> {

    override fun reduce(events: List<T>, event: T): List<T> {
        val newEventList = (events + event)
        val lastNotRevertableEvent = newEventList.lastOrNull { current ->
            status(current) == LogStatus.CONFIRMED && isNotReverted(incomeEvent = event, current = current)
        }
        return newEventList
            .filter { current ->
                // we remove all CONFIRMED logs which can't be reverted anymore,
                // except the latest not revertable logs
                // we always must have at least one not revertable log in the list
                status(current) != LogStatus.CONFIRMED ||
                    current == lastNotRevertableEvent ||
                    isReverted(incomeEvent = event, current = current)
            }
    }

    override fun wasApplied(events: List<T>, event: T): Boolean {
        val lastAppliedEvent = events.lastOrNull { status(it) == LogStatus.CONFIRMED }
        return !(lastAppliedEvent == null || lastAppliedEvent < event)
    }

    private fun isReverted(incomeEvent: T, current: T): Boolean {
        return isNotReverted(incomeEvent, current).not()
    }

    private fun isNotReverted(incomeEvent: T, current: T): Boolean {
        val incomeBlockNumber = requireNotNull(blockNumber(incomeEvent))
        val currentBlockNumber = requireNotNull(blockNumber(current))
        val blockDiff = incomeBlockNumber - currentBlockNumber

        require(blockDiff >= 0) {
            "Block diff between income=$incomeEvent and current=$current can't be negative"
        }
        return blockDiff >= confirmationBlocks
    }
}
