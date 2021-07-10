package com.rarible.core.reduce.service.model

import java.math.BigInteger
import java.util.*

enum class EventType {
    INCOME_TRANSFER,
    OUTCOME_TRANSFER
}

sealed class AccountBalanceEvent(
    var type: EventType
) {
    abstract val blockNumber: Long
    abstract val bank: Address
    abstract val owner: Address
    abstract val value: BigInteger
    abstract val date: Date
}

data class AccountIncomeTransfer(
    override val blockNumber: Long,
    override val owner: Address,
    override val value: BigInteger,
    override val bank: Address,
    override val date: Date
) : AccountBalanceEvent(
    type = EventType.INCOME_TRANSFER
)

data class AccountOutcomeTransfer(
    override val blockNumber: Long,
    override val owner: Address,
    override val value: BigInteger,
    override val bank: Address,
    override val date: Date
) : AccountBalanceEvent(
    type = EventType.OUTCOME_TRANSFER
)