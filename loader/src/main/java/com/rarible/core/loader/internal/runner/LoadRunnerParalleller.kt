package com.rarible.core.loader.internal.runner

import com.rarible.core.loader.internal.common.KafkaLoadTaskId
import com.rarible.core.loader.internal.common.LoadParalleller

class LoadRunnerParalleller(
    numberOfThreads: Int,
    loadRunner: LoadRunner
) : LoadParalleller<KafkaLoadTaskId>(
    numberOfThreads = numberOfThreads,
    threadPrefix = "load-runner",
    runner = { taskId -> loadRunner.load(taskId.id) }
)
