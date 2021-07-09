package com.rarible.core.reduce.service

import com.rarible.core.reduce.AbstractIntegrationTest
import com.rarible.core.reduce.service.repository.AccountBalanceRepository
import com.rarible.core.reduce.service.repository.AccountBalanceSnapshotRepository
import com.rarible.core.reduce.service.repository.AccountReduceEventRepository
import com.rarible.core.reduce.service.repository.ReduceDataRepository

internal class ReduceServiceTest : AbstractIntegrationTest() {
    private val balanceRepository = AccountBalanceRepository(template)
    private val snapshotRepository = AccountBalanceSnapshotRepository(template)
    private val eventRepository = AccountReduceEventRepository(template)
    private val dataRepository = ReduceDataRepository(balanceRepository)

    private val service = ReduceService(
        reducer = null,
        eventRepository = eventRepository,
        snapshotRepository = snapshotRepository,
        dataRepository = dataRepository,
        eventsCountBeforeSnapshot = 12
    )
}