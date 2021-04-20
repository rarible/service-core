package com.rarible.core.daemon.sequential

import com.rarible.core.daemon.AbstractDaemonWorker
import com.rarible.core.daemon.DaemonWorkerProperties
import io.micrometer.core.instrument.MeterRegistry
import kotlinx.coroutines.CoroutineScope

abstract class SequentialDaemonWorker(
    meterRegistry: MeterRegistry,
    properties: DaemonWorkerProperties
) : AbstractDaemonWorker(meterRegistry, properties) {

    protected abstract suspend fun handle()

    override suspend fun run(scope: CoroutineScope) {
        loop {
            handle()
        }
    }
}
