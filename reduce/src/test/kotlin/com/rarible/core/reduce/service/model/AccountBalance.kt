package com.rarible.core.reduce.service.model

import org.springframework.data.annotation.AccessType
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.Transient
import org.springframework.data.annotation.Version
import org.springframework.data.mongodb.core.mapping.Document
import java.math.BigInteger

typealias Address = String

@Document(collection = "account_balance")
data class AccountBalance(
    val bank: Address,
    val owner: Address,
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
