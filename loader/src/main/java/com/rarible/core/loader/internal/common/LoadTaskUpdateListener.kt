package com.rarible.core.loader.internal.common

interface LoadTaskUpdateListener {

    suspend fun onTaskSaved(task: LoadTask)
}