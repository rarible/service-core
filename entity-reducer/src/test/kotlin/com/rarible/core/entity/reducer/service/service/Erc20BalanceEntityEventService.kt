package com.rarible.core.entity.reducer.service.service

import com.rarible.core.entity.reducer.service.EntityEventService
import com.rarible.core.entity.reducer.service.model.Erc20BalanceEvent

class Erc20BalanceEntityEventService : EntityEventService<Erc20BalanceEvent, Long> {
    override fun getEntityId(event: Erc20BalanceEvent): Long {
        return event.entityId
    }
}
