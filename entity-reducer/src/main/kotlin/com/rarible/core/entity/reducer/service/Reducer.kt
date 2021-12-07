package com.rarible.core.entity.reducer.service

interface Reducer<Event : Comparable<Event>, E> {
    suspend fun reduce(entity: E, event: Event): E
}

