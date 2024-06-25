package com.rarible.core.entity.reducer.service.service

import com.rarible.core.entity.reducer.service.EntityIdService
import com.rarible.core.entity.reducer.service.model.Erc20Balance
import com.rarible.core.entity.reducer.service.model.Erc20BalanceEvent

class Erc20BalanceEntityIdService : EntityIdService<Erc20Balance, Erc20BalanceEvent, Long> {
    override fun getEventEntityId(event: Erc20BalanceEvent): Long {
        return event.entityId
    }

    override fun getEntityId(entity: Erc20Balance): Long {
        return entity.id
    }
}
