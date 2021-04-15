package com.rarible.core.common

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

    interface Update {
        suspend fun update(): Boolean
    }
}