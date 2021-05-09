package com.rarible.core.daemon.sequential

import com.rarible.core.daemon.AbstractDaemonWorker
import com.rarible.core.daemon.DaemonWorkerProperties
import io.micrometer.core.instrument.MeterRegistry
import kotlinx.coroutines.CoroutineScope

abstract class SequentialDaemonWorker(
    meterRegistry: MeterRegistry,
    properties: DaemonWorkerProperties,
    workerName: String? = null
) : AbstractDaemonWorker(meterRegistry, properties, workerName) {

    protected abstract suspend fun handle()

    override suspend fun run(scope: CoroutineScope) {
        loop {
            handle()
        }
    }
}
