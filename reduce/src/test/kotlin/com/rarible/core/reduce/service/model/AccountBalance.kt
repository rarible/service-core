package com.rarible.core.reduce.service.model

import org.springframework.data.annotation.AccessType
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.Transient
import org.springframework.data.annotation.Version
import org.springframework.data.mongodb.core.mapping.Document
import java.math.BigInteger
import java.util.*

@Document(collection = "account_balance")
data class AccountBalance(
    val bank: UUID,
    val owner: UUID,
    val balance: BigInteger,
    @Version
    val version: Long? = null
) {
    @Transient
    private val _id: AccountId = AccountId(bank, owner)

    @get:Id
    @get:AccessType(AccessType.Type.PROPERTY)
    var id: AccountId
        get() = _id
        set(_) {}

    fun withBalance(balance: BigInteger): AccountBalance {
        return copy(balance = balance)
    }
}