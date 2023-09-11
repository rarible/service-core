package com.rarible.core.reduce.service.repository

import com.rarible.core.reduce.repository.SnapshotRepository
import com.rarible.core.reduce.service.model.AccountBalance
import com.rarible.core.reduce.service.model.AccountId
import com.rarible.core.reduce.service.model.AccountReduceSnapshot
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.findById

class AccountBalanceSnapshotRepository(
    private val template: ReactiveMongoTemplate
) : SnapshotRepository<AccountReduceSnapshot, AccountBalance, Long, AccountId> {

    override suspend fun get(key: AccountId): AccountReduceSnapshot? {
        return template.findById<AccountReduceSnapshot>(key).awaitFirstOrNull()
    }

    override suspend fun save(snapshot: AccountReduceSnapshot): AccountReduceSnapshot {
        return template.save(snapshot).awaitFirst()
    }
}
