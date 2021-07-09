package com.rarible.core.reduce.service.model

import com.rarible.core.reduce.model.DataKey
import java.util.*

data class AccountId(
    val bank: UUID,
    val owner: UUID
) : DataKey {
    override fun toString(): String {
        return "$bank:$owner"
    }
}

