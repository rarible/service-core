package com.rarible.core.reduce.service.repository

import com.rarible.core.reduce.repository.ReduceEventRepository
import com.rarible.core.reduce.service.model.AccountBalanceEvent
import com.rarible.core.reduce.service.model.AccountId
import com.rarible.core.reduce.service.model.AccountReduceEvent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.and
import org.springframework.data.mongodb.core.query.gt
import org.springframework.data.mongodb.core.query.isEqualTo

class AccountReduceEventRepository(
    private val template: ReactiveMongoTemplate
) : ReduceEventRepository<AccountReduceEvent, Long, AccountId> {

    suspend fun dropCollection() {
        template.dropCollection(AccountBalanceEvent::class.java).awaitFirstOrNull()
    }

    suspend fun save(event: AccountBalanceEvent): AccountBalanceEvent {
        return template.save(event).awaitFirst()
    }

    override fun getEvents(key: AccountId?, after: Long?): Flow<AccountReduceEvent> {
        val criteria = listOfNotNull(
            key?.let { (AccountBalanceEvent::bank isEqualTo key.bank).and(AccountBalanceEvent::owner).isEqualTo(key.owner) },
            after?.let { AccountBalanceEvent::blockNumber gt after }
        )
        val query = Query()

        criteria.forEach { query.addCriteria(it) }
        query.with(Sort.by(Sort.Direction.ASC, AccountBalanceEvent::blockNumber.name))

        return template
            .find(query, AccountBalanceEvent::class.java)
            .map { AccountReduceEvent(it) }
            .asFlow()
    }
}
