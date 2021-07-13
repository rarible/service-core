package com.rarible.core.reduce.repository

interface DataRepository<Data> {
    suspend fun saveReduceResult(data: Data)
}