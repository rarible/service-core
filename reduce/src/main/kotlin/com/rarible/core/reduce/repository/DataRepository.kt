package com.rarible.core.reduce.repository

interface DataRepository<Data> {
    suspend fun save(data: Data)
}