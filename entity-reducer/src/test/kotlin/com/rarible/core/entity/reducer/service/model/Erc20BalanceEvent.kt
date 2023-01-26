package com.rarible.core.entity.reducer.service.model

import com.rarible.core.common.nowMillis
import com.rarible.core.test.data.randomLong
import java.time.Instant

data class Erc20BalanceEvent(
    val block: Long,
    val value: Int = 0,
    val entityId: Long = 0,
    val date: Instant = nowMillis().minusMillis(randomLong(1000))
)
