package com.rarible.core.entity.reducer.service

import kotlinx.coroutines.flow.Flow

interface ReduceService<Id, Event, E> {
    fun reduce(events: Flow<Event>): Flow<E>
    fun reduceFromState(initialState: E?, events: Flow<Event>): Flow<E>
}
