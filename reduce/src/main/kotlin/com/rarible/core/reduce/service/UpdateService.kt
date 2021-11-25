package com.rarible.core.reduce.service

interface UpdateService<Data> {
    suspend fun update(data: Data)
}
