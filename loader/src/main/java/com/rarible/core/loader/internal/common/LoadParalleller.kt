package com.rarible.core.loader.internal.common

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.isActive
import org.jetbrains.annotations.TestOnly
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

abstract class LoadParalleller<T>(
    numberOfThreads: Int,
    private val threadPrefix: String,
    private val runner: suspend (T) -> Unit
) : AutoCloseable {

    private val daemonDispatcher = Executors.newFixedThreadPool(numberOfThreads) { runnable ->
        Thread(runnable, "$threadPrefix-${THREAD_INDEX.getAndIncrement()}").apply {
            isDaemon = true
        }
    }.asCoroutineDispatcher()

    private val scope = CoroutineScope(
        SupervisorJob() + daemonDispatcher
    )

    val isActive: Boolean get() = scope.isActive

    suspend fun load(elements: List<T>) {
        elements.map { scope.async { runner(it) } }.awaitAll()
    }

    @TestOnly
    fun cancelAll() {
        scope.coroutineContext[Job]?.cancelChildren()
    }

    override fun close() {
        scope.cancel()
        daemonDispatcher.close()
    }

    private companion object {
        val THREAD_INDEX = AtomicInteger()
    }
}
