package com.rarible.core.entity.reducer.service

import com.rarible.core.entity.reducer.exception.ReduceException
import com.rarible.core.entity.reducer.model.RevertableEntity

class RevertableEntityReversedReducer<Id, Event : Comparable<Event>, E : RevertableEntity<Id, Event, E>>(
    private val reversedReducer: ReversedReducer<Event, E>
) : Reducer<Event, E> {

    override suspend fun reduce(entity: E, event: Event): E {
        // We can revert any income event witch in entity event list
        return if (entity.events.any { it.compareTo(event) == 0 }) {
            reversedReducer.reduce(entity, event).withEvents(entity.events - event)
        } else {
            val firstEvent = entity.events.firstOrNull()

            if (firstEvent == null || firstEvent > event) {
                throw ReduceException("Unable to revert $event from $entity")
            } else {
                entity
            }
        }
    }
}
