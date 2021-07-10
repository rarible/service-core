package com.rarible.core.reduce.service.repository

import com.rarible.core.reduce.repository.ReduceEventRepository
import com.rarible.core.reduce.service.model.AccountBalanceEvent
import com.rarible.core.reduce.service.model.AccountId
import com.rarible.core.reduce.service.model.AccountReduceEvent
import kotlinx.coroutines.reactive.awaitFirst
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.query.*
import reactor.core.publisher.Flux

class AccountReduceEventRepository(
    private val template: ReactiveMongoTemplate
) : ReduceEventRepository<AccountReduceEvent, Long, AccountId> {

    suspend fun save(event: AccountBalanceEvent): AccountBalanceEvent {
        return template.save(event).awaitFirst()
    }

    override fun getEvents(key: AccountId?, after: Long?): Flux<AccountReduceEvent> {
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
    }
}