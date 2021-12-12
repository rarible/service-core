package com.rarible.core.entity.reducer.service.service

import com.rarible.core.entity.reducer.service.EntityIdService
import com.rarible.core.entity.reducer.service.model.Erc20BalanceEvent

class Erc20BalanceEntityIdService : EntityIdService<Erc20BalanceEvent, Long> {
    override fun getEntityId(event: Erc20BalanceEvent): Long {
        return event.entityId
    }
}
