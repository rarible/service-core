package com.rarible.core.daemon

import com.rarible.core.daemon.healthcheck.LivenessHealthIndicator
import com.rarible.core.daemon.healthcheck.TouchLivenessHealthIndicator
import io.micrometer.core.instrument.MeterRegistry
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.boot.actuate.health.Health
import org.springframework.boot.actuate.health.HealthIndicator
import java.time.Duration
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicLong

abstract class AbstractDaemonWorker(
    protected val meterRegistry: MeterRegistry,
    properties: DaemonWorkerProperties,
    workerName: String? = null,
    private val completionHandler: CompletionHandler? = null
) : AutoCloseable, HealthIndicator {

    protected val logger: Logger = LoggerFactory.getLogger(this::class.java)
    protected val workerName = workerName ?: this::class.simpleName!!

    protected val errorDelay = properties.errorDelay
    protected val pollingPeriod = properties.pollingPeriod

    protected open val healthCheck: LivenessHealthIndicator = TouchLivenessHealthIndicator(
        maxOf(errorDelay, pollingPeriod) + Duration.ofMinutes(3)
    )

    private val daemonDispatcher = Executors
        .newSingleThreadExecutor { r ->
            Thread(r, "DaemonWorker-$workerName-${DAEMON_DISPATCHER_INDEX.getAndIncrement()}").apply {
                isDaemon = true
            }
        }.asCoroutineDispatcher()

    private val scope = CoroutineScope(SupervisorJob() + daemonDispatcher)

    private val job = scope.launch(start = CoroutineStart.LAZY) { run(this) }

    protected val isCancelled: Boolean get() = job.isCancelled

    /**
     * Daemon worker logic to be implemented.
     */
    protected abstract suspend fun run(scope: CoroutineScope)

    fun start() {
        logger.info("Starting daemon worker $workerName")

        check(job.start()) { "Daemon worker was already started $workerName" }
        job.invokeOnCompletion { error ->
            when (error) {
                is CancellationException -> logger.info("Daemon worker cancelled $workerName")
                null -> logger.warn("Daemon worker finished $workerName")
                else -> logger.error("Daemon worker failed $workerName", error)
            }
            completionHandler?.invoke(error)
        }
    }

    override fun close() {
        logger.info("Stopping the daemon worker $workerName")
        job.cancel()
    }

    override fun health(): Health = healthCheck.health()

    private companion object {
        private val DAEMON_DISPATCHER_INDEX = AtomicLong(0)
    }
}
