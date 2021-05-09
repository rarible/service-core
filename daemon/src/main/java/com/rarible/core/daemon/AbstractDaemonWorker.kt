package com.rarible.core.daemon

import com.rarible.core.daemon.healthcheck.LivenessHealthIndicator
import com.rarible.core.daemon.healthcheck.TouchLivenessHealthIndicator
import com.rarible.core.telemetry.metrics.increment
import io.micrometer.core.instrument.MeterRegistry
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.time.delay
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.boot.actuate.health.Health
import org.springframework.boot.actuate.health.HealthIndicator
import java.time.Duration
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicLong
import kotlin.coroutines.coroutineContext
import kotlin.system.exitProcess

abstract class AbstractDaemonWorker(
    private val meterRegistry: MeterRegistry,
    properties: DaemonWorkerProperties,
    workerName: String? = null
) : AutoCloseable, HealthIndicator {

    protected val logger: Logger = LoggerFactory.getLogger(this::class.java)
    protected val workerName = workerName ?: defaultName()

    protected val errorDelay = properties.errorDelay
    protected val pollingPeriod = properties.pollingPeriod

    protected open val healthCheck: LivenessHealthIndicator = TouchLivenessHealthIndicator(
        maxOf(errorDelay, pollingPeriod) + Duration.ofMinutes(3)
    )

    protected open val completionHandler: CompletionHandler = { error ->
        when (error) {
            is CancellationException -> logger.info("Job cancelled")

            null -> {
                logger.warn("Job finished")
                exitProcess(0)
            }

            else -> {
                logger.error("Job failed", error)
                exitProcess(1)
            }
        }
    }

    private val daemonDispatcher = Executors
        .newSingleThreadExecutor { r ->
            Thread(r, "DaemonWorker-${DAEMON_DISPATCHER_INDEX.getAndIncrement()}").apply {
                isDaemon = true
            }
        }.asCoroutineDispatcher()

    private val job = GlobalScope.launch(daemonDispatcher, start = CoroutineStart.LAZY) { run(this) }

    fun start() {
        logger.info("Run $workerName")

        job.start()
        job.invokeOnCompletion(completionHandler)
    }

    override fun close() {
        job.cancel()
    }

    override fun health(): Health = healthCheck.health()

    protected abstract suspend fun run(scope: CoroutineScope)

    protected suspend fun loop(job: suspend () -> Unit) {
        while (coroutineContext.isActive) {
            try {
                job()
            } catch (ex: CancellationException) {
                throw ex
            } catch (ex: Exception) {
                logger.error("Execution exception", ex)
                meterRegistry.increment(DaemonError(workerName))
                delay(errorDelay)
            }

            healthCheck.up()
            meterRegistry.increment(DaemonLiveness(workerName))
        }
    }

    private fun defaultName(): String = this::class.simpleName!!.decapitalize()

    private companion object {
        private val DAEMON_DISPATCHER_INDEX = AtomicLong(0)
    }
}
