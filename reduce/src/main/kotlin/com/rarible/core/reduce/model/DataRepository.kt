package com.rarible.core.reduce.model

interface DataRepository<Data> {
    suspend fun save(data: Data)
}