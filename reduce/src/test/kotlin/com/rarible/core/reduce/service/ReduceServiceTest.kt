package com.rarible.core.reduce.service

import com.rarible.core.reduce.AbstractIntegrationTest
import com.rarible.core.reduce.blockchain.BlockchainSnapshotStrategy
import com.rarible.core.reduce.factory.createAccountId
import com.rarible.core.reduce.factory.createAccountIncomeTransfer
import com.rarible.core.reduce.factory.createAccountOutcomeTransfer
import com.rarible.core.reduce.service.model.AccountBalance
import com.rarible.core.reduce.service.model.AccountId
import com.rarible.core.reduce.service.model.AccountReduceEvent
import com.rarible.core.reduce.service.model.AccountReduceSnapshot
import com.rarible.core.reduce.service.reducer.AccountBalanceReducer
import com.rarible.core.reduce.service.repository.AccountBalanceRepository
import com.rarible.core.reduce.service.repository.AccountBalanceSnapshotRepository
import com.rarible.core.reduce.service.repository.AccountReduceEventRepository
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactor.asFlux
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.math.BigInteger

internal class ReduceServiceTest : AbstractIntegrationTest() {
    private val balanceRepository = AccountBalanceRepository(template)
    private val snapshotRepository = AccountBalanceSnapshotRepository(template)
    private val snapshotStrategy = BlockchainSnapshotStrategy<AccountReduceSnapshot, AccountBalance, AccountId>(4)
    private val eventRepository = AccountReduceEventRepository(template)
    private val updateService = ReduceDataUpdateService(balanceRepository)
    private val reducer = AccountBalanceReducer(balanceRepository)

    private val service = ReduceService(
        reducer = reducer,
        eventRepository = eventRepository,
        updateService = updateService,
        snapshotRepository = snapshotRepository,
        snapshotStrategy = snapshotStrategy
    )

    @Test
    fun `should get all events by account id`() = runBlocking<Unit> {
        val accountId = createAccountId()

        val event1 = createAccountIncomeTransfer(accountId).copy(value = BigInteger.valueOf(10), blockNumber = 3)
        val event2 = createAccountIncomeTransfer(accountId).copy(value = BigInteger.valueOf(5), blockNumber = 1)
        val event3 = createAccountOutcomeTransfer(accountId).copy(value = BigInteger.valueOf(7), blockNumber = 4)
        val event4 = createAccountOutcomeTransfer(accountId).copy(value = BigInteger.valueOf(7), blockNumber = 2)

        listOf(event1, event2, event3, event4, createAccountOutcomeTransfer()).forEach {
            eventRepository.save(it)
        }

        val eventList = eventRepository.getEvents(reducer.getDataKeyFromEvent(AccountReduceEvent(event1)), null).asFlux().collectList().awaitFirst()
        assertThat(eventList).hasSize(4)
        assertThat(eventList[0].event.blockNumber).isEqualTo(1)
        assertThat(eventList[1].event.blockNumber).isEqualTo(2)
        assertThat(eventList[2].event.blockNumber).isEqualTo(3)
        assertThat(eventList[3].event.blockNumber).isEqualTo(4)
    }

    @Test
    fun `should reduce events for all accounts`() = runBlocking<Unit> {
        val accountId1 = createAccountId()
        val accountId2 = createAccountId()

        val event1 = createAccountIncomeTransfer(accountId1).copy(value = BigInteger.valueOf(10), blockNumber = 3)
        val event2 = createAccountOutcomeTransfer(accountId1).copy(value = BigInteger.valueOf(9), blockNumber = 4)
        val event3 = createAccountIncomeTransfer(accountId2).copy(value = BigInteger.valueOf(10), blockNumber = 3)
        val event4 = createAccountOutcomeTransfer(accountId2).copy(value = BigInteger.valueOf(9), blockNumber = 4)

        listOf(event1, event2, event3, event4, createAccountOutcomeTransfer()).forEach {
            eventRepository.save(it)
        }
        service.onEvents(
            listOf(
                AccountReduceEvent(event1),
                AccountReduceEvent(event2),
                AccountReduceEvent(event3),
                AccountReduceEvent(event4)
            )
        )
        val balance1 = balanceRepository.get(accountId1)
        assertThat(balance1?.balance).isEqualTo(BigInteger.ONE)

        val balance2 = balanceRepository.get(accountId2)
        assertThat(balance2?.balance).isEqualTo(BigInteger.ONE)
    }

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

    @Test
    fun `should make latest snapshot after full reduce`() = runBlocking<Unit> {
        val accountId = createAccountId()
        val incomes = createSeqAccountIncomeReduceEvent(accountId, income = 1, amount = 10, startBlockNumber = 0)
        val outcomes = createSeqAccountOutcomeReduceEvent(accountId, outcome = 1, amount = 10, startBlockNumber = 10)

        (incomes + outcomes).forEach { eventRepository.save(it.event) }

        service.onEvents(incomes)

        val accountBalance1 = balanceRepository.get(accountId)
        assertThat(accountBalance1?.balance).isEqualTo(BigInteger.valueOf(0))

        val snapshot = snapshotRepository.get(accountId)
        assertThat(snapshot?.mark).isEqualTo(17)
        assertThat(snapshot?.data?.balance).isEqualTo(3)
    }

    @Test
    fun `should make latest snapshot if enough block conformations`() = runBlocking<Unit> {
        val accountId = createAccountId()
        val incomes = createSeqAccountIncomeReduceEvent(accountId, income = 1, amount = 1, startBlockNumber = 0)
        val outcomes = createSeqAccountOutcomeReduceEvent(accountId, outcome = 1, amount = 1, startBlockNumber = 20)

        (incomes + outcomes).forEach { eventRepository.save(it.event) }

        service.onEvents(incomes)

        val accountBalance1 = balanceRepository.get(accountId)
        assertThat(accountBalance1?.balance).isEqualTo(BigInteger.valueOf(0))

        val snapshot = snapshotRepository.get(accountId)
        assertThat(snapshot?.mark).isEqualTo(1)
        assertThat(snapshot?.data?.balance).isEqualTo(1)
    }

    @Test
    fun `should make latest snapshot if enough block conformations, complex test`() = runBlocking<Unit> {
        val accountId = createAccountId()
        val incomes = createSeqAccountIncomeReduceEvent(accountId, income = 1, amount = 2, startBlockNumber = 0)
        val outcomes = createSeqAccountOutcomeReduceEvent(accountId, outcome = 1, amount = 1, startBlockNumber = 30)

        (incomes + outcomes).forEach { eventRepository.save(it.event) }

        service.onEvents(incomes)

        val accountBalance1 = balanceRepository.get(accountId)
        assertThat(accountBalance1?.balance).isEqualTo(BigInteger.valueOf(1))

        val snapshot = snapshotRepository.get(accountId)
        assertThat(snapshot?.mark).isEqualTo(2)
        assertThat(snapshot?.data?.balance).isEqualTo(2)
    }

    @Test
    fun `should use latest snapshot for next reduce`() = runBlocking<Unit> {
        val accountId = createAccountId()
        val incomes = createSeqAccountIncomeReduceEvent(accountId, income = 1, amount = 10, startBlockNumber = 0)
        val outcomes = createSeqAccountOutcomeReduceEvent(accountId, outcome = 1, amount = 10, startBlockNumber = 10)

        (incomes + outcomes).forEach { eventRepository.save(it.event) }

        service.onEvents(incomes)

        eventRepository.dropCollection()

        val oldOtherIncomes = createSeqAccountIncomeReduceEvent(accountId, income = 1000, amount = 10, startBlockNumber = 0)
        val newIncomes = createSeqAccountIncomeReduceEvent(accountId, income = 1, amount = 2, startBlockNumber = 18)

        (oldOtherIncomes + newIncomes).forEach { eventRepository.save(it.event) }

        service.onEvents(newIncomes)

        val accountBalance1 = balanceRepository.get(accountId)
        assertThat(accountBalance1?.balance).isEqualTo(BigInteger.valueOf(5))
    }

    @Test
    fun `should drop snapshot if come old mark`() = runBlocking<Unit> {
        val accountId = createAccountId()
        val incomes = createSeqAccountIncomeReduceEvent(accountId, income = 1, amount = 10, startBlockNumber = 0)
        val outcomes = createSeqAccountOutcomeReduceEvent(accountId, outcome = 1, amount = 10, startBlockNumber = 10)

        (incomes + outcomes).forEach { eventRepository.save(it.event) }

        service.onEvents(incomes)

        eventRepository.dropCollection()

        val newIncomes = createSeqAccountIncomeReduceEvent(accountId, income = 1, amount = 3, startBlockNumber = 0)
        val newOutcomes = createSeqAccountOutcomeReduceEvent(accountId, outcome = 1, amount = 2, startBlockNumber = 9)

        (newIncomes + newOutcomes).forEach { eventRepository.save(it.event) }

        service.onEvents(newOutcomes)

        val accountBalance1 = balanceRepository.get(accountId)
        assertThat(accountBalance1?.balance).isEqualTo(BigInteger.valueOf(1))
    }

    private fun createSeqAccountIncomeReduceEvent(
        accountId: AccountId,
        income: Long,
        amount: Long,
        startBlockNumber: Long
    ): List<AccountReduceEvent> {
        return (1..amount)
            .map {
                createAccountIncomeTransfer(accountId).copy(value = BigInteger.valueOf(income), blockNumber = startBlockNumber + it)
            }
            .map {
                AccountReduceEvent(it)
            }
    }

    private fun createSeqAccountOutcomeReduceEvent(
        accountId: AccountId,
        outcome: Long,
        amount: Long,
        startBlockNumber: Long
    ): List<AccountReduceEvent> {
        return (1..amount)
            .map {
                createAccountOutcomeTransfer(accountId).copy(value = BigInteger.valueOf(outcome), blockNumber = startBlockNumber + it)
            }
            .map {
                AccountReduceEvent(it)
            }
    }
}
