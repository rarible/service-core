package com.rarible.core.reduce.factory

import com.rarible.core.reduce.service.model.AccountBalance
import com.rarible.core.reduce.service.model.AccountId
import com.rarible.core.reduce.service.model.AccountIncomeTransfer
import com.rarible.core.reduce.service.model.AccountOutcomeTransfer
import com.rarible.core.reduce.service.model.Address
import java.math.BigInteger
import java.util.Date
import java.util.UUID

fun randomLong(): Long {
    return (1L..1000).random()
}

fun randomBigInteger(): BigInteger {
    return randomLong().toBigInteger()
}

fun randomAddress(): Address {
    return UUID.randomUUID().toString()
}

fun createAccountBalance(withId: AccountId = createAccountId()): AccountBalance {
    return AccountBalance(
        bank = withId.bank,
        owner = withId.owner,
        balance = randomBigInteger()
    )
}

fun createAccountId(): AccountId {
    return AccountId(
        bank = randomAddress(),
        owner = randomAddress()
    )
}

fun createAccountIncomeTransfer(withId: AccountId = createAccountId()): AccountIncomeTransfer {
    return AccountIncomeTransfer(
        blockNumber = randomLong(),
        bank = withId.bank,
        owner = withId.owner,
        value = randomBigInteger(),
        date = Date()
    )
}

fun createAccountOutcomeTransfer(withId: AccountId = createAccountId()): AccountOutcomeTransfer {
    return AccountOutcomeTransfer(
        blockNumber = randomLong(),
        bank = withId.bank,
        owner = withId.owner,
        value = randomBigInteger(),
        date = Date()
    )
}
