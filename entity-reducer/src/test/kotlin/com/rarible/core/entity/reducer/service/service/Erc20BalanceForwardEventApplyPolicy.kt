package com.rarible.core.entity.reducer.service.service

import com.rarible.core.entity.reducer.service.EventApplyPolicy
import com.rarible.core.entity.reducer.service.model.Erc20BalanceEvent

class Erc20BalanceForwardEventApplyPolicy : EventApplyPolicy<Erc20BalanceEvent> {
    override fun reduce(events: List<Erc20BalanceEvent>, event: Erc20BalanceEvent): List<Erc20BalanceEvent> {
        return events + event
    }

    override fun wasApplied(events: List<Erc20BalanceEvent>, event: Erc20BalanceEvent): Boolean {
        val last = events.lastOrNull()
        return !(last == null || last.block < event.value)
    }
}
