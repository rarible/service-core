package com.rarible.core.daemon.sequential

/**
 * Aggregator class that can be used to conveniently start and stop all registered
 * `ConsumerWorker`s of the Spring application.
 *
 * Usually, we start the consumers on application startup.
 * Spring will automatically close the bean since it implements the `AutoCloseable`.
 */
class ConsumerWorkerHolder<T>(
    private val consumers: List<AbstractConsumerWorker<T, *>>
) : AutoCloseable {

    fun start() {
        consumers.forEach { it.start() }
    }

    val isActive: Boolean get() = consumers.all { it.isActive }

    override fun close() {
        consumers.forEach { it.close() }
    }
}
