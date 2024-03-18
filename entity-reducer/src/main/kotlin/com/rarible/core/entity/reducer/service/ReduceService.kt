package com.rarible.core.entity.reducer.service

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.scan

interface ReduceService<Id, Event, E> {
    fun reduceWithEvents(events: Flow<Event>): Flow<EntityWithEvent<E, Event>>

    fun reduce(events: Flow<Event>): Flow<E> =
        reduceWithEvents(events)
            .scan<EntityWithEvent<E, Event>, E?>(null) { prev, (entity, _) -> if (entity == prev) null else entity }
            .filterNotNull()

    data class EntityWithEvent<E, Event>(
        val entity: E,
        val event: Event
    )
}
