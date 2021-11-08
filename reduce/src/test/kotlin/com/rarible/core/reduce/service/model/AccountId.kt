package com.rarible.core.reduce.service.model

data class AccountId(
    val bank: Address,
    val owner: Address
) {
    override fun toString(): String {
        return "$bank:$owner"
    }
}

