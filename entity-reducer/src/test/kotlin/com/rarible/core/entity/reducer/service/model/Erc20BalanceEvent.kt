package com.rarible.core.entity.reducer.service.model

data class Erc20BalanceEvent(
    val block: Long,
    val value: Int = 0,
    val entityId: Long = 0
)
