package com.rarible.core.entity.reducer.service

interface Reducer<Event, E> {
    suspend fun reduce(entity: E, event: Event): E
}

