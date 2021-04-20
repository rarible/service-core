package com.rarible.core.task

import kotlinx.coroutines.flow.Flow

interface TaskHandler<T: Any> {
    val type: String
    suspend fun isAbleToRun(param: String): Boolean {
        return true
    }
    fun getAutorunParams() = emptyList<RunTask>()
    fun runLongTask(from: T?, param: String): Flow<T>
}

data class RunTask(
    val param: String,
    val sample: Long? = Task.DEFAULT_SAMPLE
)