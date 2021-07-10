package com.rarible.core.reduce.service

import com.rarible.core.reduce.AbstractIntegrationTest
import com.rarible.core.reduce.factory.createAccountId
import com.rarible.core.reduce.factory.createAccountIncomeTransfer
import com.rarible.core.reduce.factory.createAccountOutcomeTransfer
import com.rarible.core.reduce.service.model.AccountReduceEvent
import com.rarible.core.reduce.service.reducer.AccountBalanceReducer
import com.rarible.core.reduce.service.repository.AccountBalanceRepository
import com.rarible.core.reduce.service.repository.AccountBalanceSnapshotRepository
import com.rarible.core.reduce.service.repository.AccountReduceEventRepository
import com.rarible.core.reduce.service.repository.ReduceDataRepository
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.math.BigInteger

internal class ReduceServiceTest : AbstractIntegrationTest() {
    private val balanceRepository = AccountBalanceRepository(template)
    private val snapshotRepository = AccountBalanceSnapshotRepository(template)
    private val eventRepository = AccountReduceEventRepository(template)
    private val dataRepository = ReduceDataRepository(balanceRepository)
    private val reducer = AccountBalanceReducer()

    private val service = ReduceService(
        reducer = reducer,
        eventRepository = eventRepository,
        snapshotRepository = snapshotRepository,
        dataRepository = dataRepository,
        eventsCountBeforeSnapshot = 12
    )

    @Test
    fun `should reduce simple events`() = runBlocking<Unit> {
        val accountId1 = createAccountId()
        val accountId2 = createAccountId()

        val event11 = createAccountIncomeTransfer(accountId1).copy(value = BigInteger.valueOf(10), blockNumber = 1)
        val event21 = createAccountIncomeTransfer(accountId1).copy(value = BigInteger.valueOf(5), blockNumber = 2)
        val event31 = createAccountOutcomeTransfer(accountId1).copy(value = BigInteger.valueOf(7), blockNumber = 3)

        val event12 = createAccountIncomeTransfer(accountId2).copy(value = BigInteger.valueOf(20), blockNumber = 2)
        val event22 = createAccountOutcomeTransfer(accountId2).copy(value = BigInteger.valueOf(5), blockNumber = 3)
        val event32 = createAccountOutcomeTransfer(accountId2).copy(value = BigInteger.valueOf(10), blockNumber = 4)

        val newEvents = listOf(
            AccountReduceEvent(event11),
            AccountReduceEvent(event21),
            AccountReduceEvent(event31),
            AccountReduceEvent(event12),
            AccountReduceEvent(event22),
            AccountReduceEvent(event32)
        )
        newEvents.forEach { eventRepository.save(it.event) }

        service.onEvents(newEvents)

        val accountBalance1 = balanceRepository.get(accountId1)
        assertThat(accountBalance1).isNotNull
        assertThat(accountBalance1?.balance).isEqualTo(BigInteger.valueOf(8))

        val accountBalance2 = balanceRepository.get(accountId2)
        assertThat(accountBalance2).isNotNull
        assertThat(accountBalance2?.balance).isEqualTo(BigInteger.valueOf(5))
    }
}