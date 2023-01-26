package com.rarible.core.entity.reducer.service.model

import com.rarible.core.common.nowMillis
import com.rarible.core.entity.reducer.model.Entity
import com.rarible.core.test.data.randomLong
import org.springframework.data.annotation.Id
import java.time.Instant

data class Erc20Balance(
    override val id: Long,
    val balance: Int,
    val updatedAt: Instant = nowMillis().minusMillis(randomLong(1000)),
    override val revertableEvents: List<Erc20BalanceEvent>,
    @Id
    override val version: Long? = null
) : Entity<Long, Erc20BalanceEvent, Erc20Balance> {

    override fun withRevertableEvents(events: List<Erc20BalanceEvent>): Erc20Balance {
        return copy(revertableEvents = events)
    }
}
