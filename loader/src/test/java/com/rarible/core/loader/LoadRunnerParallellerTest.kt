package com.rarible.core.loader

import com.rarible.core.loader.internal.common.KafkaLoadTaskId
import com.rarible.core.loader.internal.runner.LoadRunner
import com.rarible.core.loader.internal.runner.LoadRunnerParalleller
import com.rarible.core.loader.internal.common.generateLoadTaskId
import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.confirmVerified
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test

class LoadRunnerParallellerTest {

    private val loadRunner = mockk<LoadRunner>()

    private val loadRunnerParalleller = LoadRunnerParalleller(
        numberOfWorkingThreads = 1,
        loadRunner = loadRunner
    )

    @AfterEach
    fun closeParalleller() {
        loadRunnerParalleller.close()
    }

    @Test
    fun `run loader`() = runBlocking<Unit> {
        val loadTaskId = generateLoadTaskId()
        coJustRun { loadRunner.load(loadTaskId) }
        loadRunnerParalleller.load(listOf(KafkaLoadTaskId(loadTaskId)))
        coVerify(exactly = 1) { loadRunner.load(loadTaskId) }
        confirmVerified()
    }

    @Test
    fun `rethrow exception`() = runBlocking<Unit> {
        val loadTaskId = generateLoadTaskId()
        val error = RuntimeException("load-error")
        coEvery { loadRunner.load(loadTaskId) } throws error
        Assertions.assertThatThrownBy {
            runBlocking { loadRunnerParalleller.load(listOf(KafkaLoadTaskId(loadTaskId))) }
        }.isInstanceOf(RuntimeException::class.java)
            .hasMessage(error.localizedMessage)
        assertThat(loadRunnerParalleller.isActive).isTrue()
    }
}
