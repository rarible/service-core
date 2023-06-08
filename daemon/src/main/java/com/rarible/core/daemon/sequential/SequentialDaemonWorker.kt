package com.rarible.core.daemon.sequential

import com.rarible.core.daemon.AbstractDaemonWorker
import com.rarible.core.daemon.DaemonError
import com.rarible.core.daemon.DaemonLiveness
import com.rarible.core.daemon.DaemonWorkerProperties
import com.rarible.core.logging.withBatchId
import com.rarible.core.logging.withTraceId
import com.rarible.core.telemetry.metrics.increment
import io.micrometer.core.instrument.MeterRegistry
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.isActive
import kotlinx.coroutines.time.delay
import kotlin.coroutines.coroutineContext

abstract class SequentialDaemonWorker(
    meterRegistry: MeterRegistry,
    properties: DaemonWorkerProperties,
    workerName: String? = null,
    completionHandler: CompletionHandler? = null
) : AbstractDaemonWorker(meterRegistry, properties, workerName, completionHandler) {

    protected abstract suspend fun handle()

    override suspend fun run(scope: CoroutineScope) {
        withBatchId {
            while (coroutineContext.isActive) {
                withTraceId {
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
    }
}
