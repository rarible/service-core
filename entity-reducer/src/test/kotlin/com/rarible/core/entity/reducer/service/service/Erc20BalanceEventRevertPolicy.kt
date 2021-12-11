package com.rarible.core.entity.reducer.service.service

import com.rarible.core.entity.reducer.service.EventRevertPolicy
import com.rarible.core.entity.reducer.service.model.Erc20BalanceEvent

class Erc20BalanceEventRevertPolicy : EventRevertPolicy<Erc20BalanceEvent> {
    override fun add(event: Erc20BalanceEvent, events: List<Erc20BalanceEvent>): List<Erc20BalanceEvent> {
        return events + event
    }

    override fun remove(event: Erc20BalanceEvent, events: List<Erc20BalanceEvent>): List<Erc20BalanceEvent> {
        return events - event
    }

    override fun wasApplied(event: Erc20BalanceEvent, events: List<Erc20BalanceEvent>): Boolean {
        return events.any { current -> current.block == event.block }
    }
}
