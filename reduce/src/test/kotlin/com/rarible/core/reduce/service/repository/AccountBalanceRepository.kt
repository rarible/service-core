package com.rarible.core.reduce.service.repository

import com.rarible.core.reduce.service.model.AccountBalance
import com.rarible.core.reduce.service.model.AccountId
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.springframework.data.mongodb.core.ReactiveMongoTemplate

class AccountBalanceRepository(
    private val template: ReactiveMongoTemplate
) {
    suspend fun save(accountBalance: AccountBalance): AccountBalance {
        return template.save(accountBalance).awaitFirst()
    }

    suspend fun get(id: AccountId): AccountBalance? {
        return template.findById(id, AccountBalance::class.java).awaitFirstOrNull()
    }
}