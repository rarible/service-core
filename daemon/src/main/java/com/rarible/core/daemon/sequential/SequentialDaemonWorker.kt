package com.rarible.core.daemon.sequential

import com.rarible.core.daemon.AbstractDaemonWorker
import com.rarible.core.daemon.DaemonError
import com.rarible.core.daemon.DaemonLiveness
import com.rarible.core.daemon.DaemonWorkerProperties
import com.rarible.core.telemetry.metrics.increment
import io.micrometer.core.instrument.MeterRegistry
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.isActive
import kotlinx.coroutines.time.delay
import kotlin.coroutines.coroutineContext

abstract class SequentialDaemonWorker(
    meterRegistry: MeterRegistry,
    properties: DaemonWorkerProperties,
    workerName: String? = null
) : AbstractDaemonWorker(meterRegistry, properties, workerName) {

    protected abstract suspend fun handle()

    override suspend fun run(scope: CoroutineScope) {
        while (coroutineContext.isActive) {
            try {
                handle()
            } catch (ex: CancellationException) {
                throw ex
            } catch (ex: Exception) {
                logger.error("Daemon worker execution exception $workerName", ex)
                meterRegistry.increment(DaemonError(workerName))
                delay(errorDelay)
            }
            healthCheck.up()
            meterRegistry.increment(DaemonLiveness(workerName))
        }
    }
}
