package com.rarible.core.daemon.job

interface JobHandler {
    suspend fun handle()
}