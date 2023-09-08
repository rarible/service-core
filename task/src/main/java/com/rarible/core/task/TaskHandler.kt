package com.rarible.core.task

import kotlinx.coroutines.flow.Flow

/**
 * Background handler of tasks of a specific [type]. Tasks are run by [TaskService].
 * There may be several tasks of this type with different `param` running in the background simultaneously.
 * Task is unique by a [type] and `param` with which it was started.
 * Task completion state returned from [runLongTask] and is recorded in the database to resume execution from the last state.
 *
 * Override this interface in a class marked with `@Component` or `@Service`.
 * Spring will pick up and register this handler automatically.
 *
 * There are 2 options how to schedule this task for execution:
 * - run [TaskService.runTask] with `type` parameter equal to this handler's [type] and a parameter to run with.
 * - override [getAutorunParams] with non-empty list of params. [TaskService] will schedule the task for execution
 * after a fixed delay on the application startup (usually after 30 seconds).
 *
 * **Note!** `<T>` type parameter can be only of types `String | Long | Int | MyDataClass`.
 * Do not use `Address | EthUInt256` or other types that internally serialize to string, but cannot be deserialized without a hint.
 * This is because the current implementation of `TaskRunner` loses type information of the state field (RPN-1164).
 */
interface TaskHandler<T : Any> {
    /**
     * Application-wide unique identifier of this type of tasks.
     */
    val type: String

    /**
     * Whether this task should be run right now.
     */
    suspend fun isAbleToRun(param: String): Boolean {
        return true
    }

    /**
     * Parameters for auto-running the task after a fixed delay on application startup.
     * Override this function to automatically start the background task.
     */
    fun getAutorunParams() = emptyList<RunTask>()

    /**
     * Task execution logic.
     * The task may be resumed [from] the latest state, which is recorded in the database.
     * The task state is updated every time the resulting `Flow<T>` emits the next element.
     */
    fun runLongTask(from: T?, param: String): Flow<T>
}

/**
 * Auto-run parameters of the task, used as return type [TaskHandler.getAutorunParams].
 */
data class RunTask(
    val param: String,
    val sample: Long? = Task.DEFAULT_SAMPLE
)
