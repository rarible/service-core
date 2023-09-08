package com.rarible.core.reduce.service.model

import com.rarible.core.reduce.model.ReduceEvent

data class AccountReduceEvent(
    val event: AccountBalanceEvent
) : ReduceEvent<Long> {
    override val mark = event.blockNumber
}
