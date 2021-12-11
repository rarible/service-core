package com.rarible.core.entity.reducer.service.model

import com.rarible.core.entity.reducer.model.RevertableEntity

data class Erc20Balance(
    override val id: Long,
    val balance: Int,
    override val revertableEvents: List<Erc20BalanceEvent>,
) : RevertableEntity<Long, Erc20BalanceEvent, Erc20Balance> {

    override fun withRevertableEvents(events: List<Erc20BalanceEvent>): Erc20Balance {
        return copy(revertableEvents = events)
    }
}
