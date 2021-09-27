package com.rarible.core.reduce.blockchain

import com.rarible.core.reduce.factory.createAccountBalance
import com.rarible.core.reduce.factory.createAccountId
import com.rarible.core.reduce.service.model.AccountBalance
import com.rarible.core.reduce.service.model.AccountId
import com.rarible.core.reduce.service.model.AccountReduceSnapshot
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.lang.IllegalArgumentException

internal class BlockchainSnapshotStrategyTest {
    private val numberOfBlockConfirmations = 12
    private val snapshotStrategy = BlockchainSnapshotStrategy<AccountReduceSnapshot, AccountBalance, AccountId>(numberOfBlockConfirmations)
    private val initSnapshot = AccountReduceSnapshot(createAccountId(), createAccountBalance(), 1)

    @Test
    fun `should validate next mark`() {
        val cxt = snapshotStrategy.context(initSnapshot)

        assertThrows<IllegalArgumentException> {
            cxt.validate(-1)
        }
    }

    @Test
    fun `should get next snapshot to save if all conformation blocks arrived`() {
        val cxt = snapshotStrategy.context(initSnapshot)
        val snapshots = (1L..numberOfBlockConfirmations).map {
            AccountReduceSnapshot(createAccountId(), createAccountBalance(), it)
        }
        snapshots.forEach { cxt.push(it) }
        assertThat(cxt.needSave()).isTrue()
        assertThat(cxt.next()).isEqualTo(snapshots.first())
    }

    @Test
    fun `should get next snapshot to save if more then nu,ber conformation blocks arrived`() {
        val cxt = snapshotStrategy.context(initSnapshot)
        val snapshots = (1L..numberOfBlockConfirmations * 2).map {
            AccountReduceSnapshot(createAccountId(), createAccountBalance(), it)
        }
        snapshots.forEach { cxt.push(it) }
        assertThat(cxt.needSave()).isTrue()
        assertThat(cxt.next()).isEqualTo(snapshots[numberOfBlockConfirmations])
    }

    @Test
    fun `should not need save if not enough block conformations`() {
        val cxt = snapshotStrategy.context(initSnapshot)
        val snapshots = (1L..numberOfBlockConfirmations - 2).map {
            AccountReduceSnapshot(createAccountId(), createAccountBalance(), it)
        }
        snapshots.forEach { cxt.push(it) }
        assertThat(cxt.needSave()).isFalse()
    }

    @Test
    fun `should get next snapshot is distance between blocks is enough`() {
        val cxt = snapshotStrategy.context(initSnapshot)
        val snap1 = AccountReduceSnapshot(createAccountId(), createAccountBalance(), 5)
        val snap2 = AccountReduceSnapshot(createAccountId(), createAccountBalance(), 24)
        cxt.push(snap1)
        cxt.push(snap2)

        assertThat(cxt.needSave()).isTrue()
        assertThat(cxt.next()).isEqualTo(snap1)
    }

    @Test
    fun `should not get next snapshot if not enough blocks arrived`() {
        val cxt = snapshotStrategy.context(initSnapshot)
        val snap1 = AccountReduceSnapshot(createAccountId(), createAccountBalance(), 24)
        cxt.push(snap1)

        assertThat(cxt.needSave()).isFalse()
    }

    @Test
    fun `should not save if not changes`() {
        val cxt = snapshotStrategy.context(initSnapshot)
        assertThat(cxt.needSave()).isFalse()
    }
}
