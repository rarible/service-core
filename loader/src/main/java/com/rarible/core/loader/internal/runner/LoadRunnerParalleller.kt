package com.rarible.core.loader.internal.runner

import com.rarible.core.loader.internal.common.KafkaLoadTaskId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

class LoadRunnerParalleller(
    numberOfWorkingThreads: Int,
    private val loadRunner: LoadRunner
) : AutoCloseable {

    private val daemonDispatcher = Executors
        .newFixedThreadPool(numberOfWorkingThreads) { runnable ->
            Thread(runnable, "load-runner-${LOAD_RUNNER_THREAD_INDEX.getAndIncrement()}").apply {
                isDaemon = true
            }
        }
        .asCoroutineDispatcher()

    private val scope = CoroutineScope(
        SupervisorJob() + daemonDispatcher
    )

    val isActive: Boolean get() = scope.isActive

    suspend fun load(loadTasks: List<KafkaLoadTaskId>) {
        loadTasks.map { scope.async { loadRunner.load(it.id) } }.awaitAll()
    }

    override fun close() {
        scope.cancel()
        daemonDispatcher.close()
    }

    private companion object {
        val LOAD_RUNNER_THREAD_INDEX = AtomicInteger()
    }
}
