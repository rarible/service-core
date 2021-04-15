package com.rarible.core.task

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.reactive.asFlow
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import reactor.core.publisher.FluxSink
import reactor.core.publisher.ReplayProcessor
import java.util.*
import kotlin.collections.ArrayList

class MockHandler(
    override val type: String
) : TaskHandler<Int> {
    private val messages = ReplayProcessor.create<Int>()
    val sink: FluxSink<Int> = messages.sink()

    override fun runLongTask(resume: Int?, param: String): Flow<Int> =
        messages.asFlow()
}

@Component
class TaskRunnerEventListener {
    val runnerEvents = Collections.synchronizedList(ArrayList<TaskRunnerEvent>())

    @EventListener
    fun onEvent(event: TaskRunnerEvent) {
        runnerEvents.add(event)
    }
}