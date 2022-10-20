package com.rarible.protocol.nft.core.service.policy

import com.rarible.core.entity.reducer.service.EventApplyPolicy
import com.rarible.core.entity.reducer.service.LogStatus

open class RevertEventApplyPolicy<T : Comparable<T>>(
    private val status: (T) -> LogStatus,
) : EventApplyPolicy<T> {
    override fun reduce(events: List<T>, event: T): List<T> {
        val confirmedEvents = events.filter {
            status(it) == LogStatus.CONFIRMED
        }
        require(confirmedEvents.isNotEmpty()) {
            "Can't revert from empty list (event=$event)"
        }
        require(event >= confirmedEvents.first()) {
            "Can't revert to old event (events=$events, event=$event)"
        }
        val confirmedEvent = findConfirmedEvent(confirmedEvents, event)
        return if (confirmedEvent != null) {
            //TODO: back after bug in blockchain scanner will be fixed
//            require(events.last() == confirmedEvent) {
//                "Event must revert from tail of list. Revert event: $event, event list=$events"
//            }
            events - confirmedEvent
        } else events
    }

    override fun wasApplied(events: List<T>, event: T): Boolean {
        return findConfirmedEvent(events, event) != null
    }

    private fun findConfirmedEvent(events: List<T>, event: T): T? {
        return events.firstOrNull { current ->
            status(current) == LogStatus.CONFIRMED && current.compareTo(event) == 0
        }
    }
}
