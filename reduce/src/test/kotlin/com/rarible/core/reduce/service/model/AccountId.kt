package com.rarible.core.reduce.service.model

import com.rarible.core.reduce.model.DataKey

data class AccountId(
    val bank: Address,
    val owner: Address
) : DataKey {
    override fun toString(): String {
        return "$bank:$owner"
    }
}

