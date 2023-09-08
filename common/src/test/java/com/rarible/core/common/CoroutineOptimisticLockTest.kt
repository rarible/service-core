package com.rarible.core.common

import com.rarible.core.test.data.randomString
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.dao.OptimisticLockingFailureException

class CoroutineOptimisticLockTest {
    @Test
    fun `should return successfully`() = runBlocking {
        val updateMethod = mockk<Update> {
            coEvery { update() }
                .throws(OptimisticLockingFailureException(""))
                .andThenThrows(OptimisticLockingFailureException(""))
                .andThen(true)
        }

        val result = optimisticLock {
            updateMethod.update()
        }

        assertThat(result).isTrue()
        coVerify(exactly = 3) { updateMethod.update() }
    }

    @Test
    fun `should failure update`() = runBlocking {
        val updateMethod = mockk<Update> {
            coEvery { update() } throws OptimisticLockingFailureException("")
        }

        assertThrows<OptimisticLockingFailureException> {
            runBlocking {
                optimisticLock {
                    updateMethod.update()
                }
            }
        }
        coVerify(exactly = 5) { updateMethod.update() }
    }

    @Test
    fun `update latest - ok`() = runBlocking<Unit> {
        val service = mockk<Update>()
        val latest = "1"
        val updated = "2"

        coEvery { service.update(latest) } returns updated

        val result = optimisticLock(
            latest = latest,
            getLatest = { service.get(it) },
            update = { service.update(it) }
        )
        assertThat(result).isEqualTo(updated)

        coVerify(exactly = 0) { service.get(any()) }
    }

    @Test
    fun `update with get latest - ok`() = runBlocking<Unit> {
        val service = mockk<Update>()
        val latest = "1"
        val newLatest = "2"
        val updated = "3"

        coEvery { service.get(latest) } returns newLatest
        coEvery { service.update(latest) } throws OptimisticLockingFailureException("")
        coEvery { service.update(newLatest) } returns updated

        val result = optimisticLock(
            latest = latest,
            getLatest = { service.get(it) },
            update = { service.update(it) }
        )
        assertThat(result).isEqualTo(updated)

        coVerify(exactly = 2) { service.update(any()) }
        coVerify(exactly = 1) { service.get(latest) }
    }

    @Test
    fun `update latest - fail`() = runBlocking<Unit> {
        val service = mockk<Update>()

        coEvery { service.update(any()) } throws OptimisticLockingFailureException("")
        coEvery { service.get(any()) } returns randomString()

        assertThrows<OptimisticLockingFailureException> {
            runBlocking {
                optimisticLock(
                    latest = randomString(),
                    getLatest = { service.get(it) },
                    update = { service.update(it) }
                )
            }
        }
    }

    interface Update {
        suspend fun update(): Boolean

        suspend fun update(latest: String): String

        suspend fun get(latest: String): String
    }
}
