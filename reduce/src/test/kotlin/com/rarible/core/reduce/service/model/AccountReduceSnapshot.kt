package com.rarible.core.reduce.service.model

import com.rarible.core.reduce.model.ReduceSnapshot
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document

@Document(collection = "account_snapshot")
class AccountReduceSnapshot(
    @Id
    override val id: AccountId,
    override val data: AccountBalance,
    override val mark: Long
) : ReduceSnapshot<AccountBalance, Long, AccountId>()