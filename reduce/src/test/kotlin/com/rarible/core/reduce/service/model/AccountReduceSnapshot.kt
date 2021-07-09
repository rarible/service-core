package com.rarible.core.reduce.service.model

import com.rarible.core.reduce.model.ReduceSnapshot

data class AccountReduceSnapshot(
    override val data: AccountBalance,
    override val mark: Long
) : ReduceSnapshot<AccountBalance, Long>()