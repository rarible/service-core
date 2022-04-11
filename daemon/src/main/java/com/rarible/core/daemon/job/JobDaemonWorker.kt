package com.rarible.core.daemon.job

import com.rarible.core.daemon.DaemonWorkerProperties
import com.rarible.core.daemon.sequential.SequentialDaemonWorker
import io.micrometer.core.instrument.MeterRegistry
import kotlinx.coroutines.CompletionHandler

abstract class JobDaemonWorker(
    private val jobHandler: JobHandler,
    meterRegistry: MeterRegistry,
    properties: DaemonWorkerProperties,
    workerName: String? = null,
    completionHandler: CompletionHandler? = null
) : SequentialDaemonWorker(meterRegistry, properties, workerName, completionHandler) {

    override suspend fun handle() {
        jobHandler.handle()
    }
}
