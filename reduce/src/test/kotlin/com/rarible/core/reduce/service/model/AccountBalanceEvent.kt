package com.rarible.core.reduce.service.model

import org.bson.types.ObjectId
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import java.math.BigInteger
import java.util.*

enum class EventType {
    INCOME_TRANSFER,
    OUTCOME_TRANSFER
}

@Document(collection = "account_balance_event")
sealed class AccountBalanceEvent(
    var type: EventType
) {
    abstract val id: ObjectId
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
    override val date: Date,
    @Id
    override val id: ObjectId = ObjectId()
) : AccountBalanceEvent(
    type = EventType.INCOME_TRANSFER
)

data class AccountOutcomeTransfer(
    override val blockNumber: Long,
    override val owner: Address,
    override val value: BigInteger,
    override val bank: Address,
    override val date: Date,
    @Id
    override val id: ObjectId = ObjectId()
) : AccountBalanceEvent(
    type = EventType.OUTCOME_TRANSFER
)