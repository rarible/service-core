package com.rarible.core.loader.internal

import com.rarible.core.loader.configuration.LoadProperties
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import org.springframework.stereotype.Component
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

@Component
class LoadRunnerParalleller(
    loadProperties: LoadProperties,
    private val loadRunner: LoadRunner
) : AutoCloseable {

    private val daemonDispatcher = Executors
        .newFixedThreadPool(loadProperties.workers) { runnable ->
            Thread(runnable, "load-runner-${LOAD_RUNNER_THREAD_INDEX.getAndIncrement()}").apply {
                isDaemon = true
            }
        }
        .asCoroutineDispatcher()

    // TODO[loader]: verify that this coroutines code is correct, read these articles:
    //  https://www.techyourchance.com/kotlin-coroutines-supervisorjob-async-exceptions-cancellation/
    //  https://proandroiddev.com/kotlin-coroutines-patterns-anti-patterns-f9d12984c68e
    //  https://elizarov.medium.com/coroutine-context-and-scope-c8b255d59055
    private val scope = CoroutineScope(SupervisorJob() + daemonDispatcher)

    suspend fun load(loadTasks: List<KafkaLoadTaskId>) {
        // TODO[loader]: add a test that all tasks get failed if any fails, and that scope does NOT close.
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
