package com.rarible.core.reduce.service.repository

import com.rarible.core.reduce.repository.ReduceEventRepository
import com.rarible.core.reduce.service.model.AccountBalanceEvent
import com.rarible.core.reduce.service.model.AccountId
import com.rarible.core.reduce.service.model.AccountReduceEvent
import kotlinx.coroutines.reactive.awaitFirst
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import reactor.core.publisher.Flux

class AccountReduceEventRepository(
    private val template: ReactiveMongoTemplate
) : ReduceEventRepository<AccountReduceEvent, Long, AccountId> {

    suspend fun save(event: AccountBalanceEvent): AccountBalanceEvent {
        return template.save(event).awaitFirst()
    }

    override fun getEvents(key: AccountId?, after: Long?): Flux<AccountReduceEvent> {
        TODO("Not yet implemented")
    }
}