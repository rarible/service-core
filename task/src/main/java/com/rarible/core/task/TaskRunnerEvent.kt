package com.rarible.core.task

sealed class TaskRunnerEvent {
    abstract val task: Task

    data class TaskStartEvent(override val task: Task): TaskRunnerEvent()
    data class TaskCompleteEvent(override val task: Task): TaskRunnerEvent()
    data class TaskErrorEvent(override val task: Task): TaskRunnerEvent()
}