package com.rarible.core.reduce.service.repository

import com.rarible.core.reduce.service.model.AccountBalance
import kotlinx.coroutines.reactive.awaitFirst
import org.springframework.data.mongodb.core.ReactiveMongoTemplate

class AccountBalanceRepository(
    private val template: ReactiveMongoTemplate
) {
    suspend fun save(accountBalance: AccountBalance): AccountBalance {
        return template.save(accountBalance).awaitFirst()
    }
}