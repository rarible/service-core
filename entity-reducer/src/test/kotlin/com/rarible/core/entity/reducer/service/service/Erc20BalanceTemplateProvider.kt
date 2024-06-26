package com.rarible.core.entity.reducer.service.service

import com.rarible.core.entity.reducer.service.EntityTemplateProvider
import com.rarible.core.entity.reducer.service.model.Erc20Balance

class Erc20BalanceTemplateProvider : EntityTemplateProvider<Long, Erc20Balance> {
    override fun getEntityTemplate(id: Long, version: Long?): Erc20Balance {
        return Erc20Balance(
            id = id,
            balance = 0,
            revertableEvents = emptyList(),
            version = version
        )
    }

    override fun getEntityTemplateFromEntity(entity: Erc20Balance, version: Long?): Erc20Balance {
        return entity.copy(
            version = version,
        )
    }
}
