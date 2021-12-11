package com.rarible.core.entity.reducer.service

import com.rarible.core.entity.reducer.model.RevertableEntity

class RevertableEntityReducer<Id, Event, E : RevertableEntity<Id, Event, E>>(
    private val eventRevertPolicy: EventRevertPolicy<Event>,
    private val reversedReducer: Reducer<Event, E>
) : Reducer<Event, E> {

    override suspend fun reduce(entity: E, event: Event): E {
        // We can revert any income event witch in entity event list
        return if (eventRevertPolicy.wasApplied(event, entity.revertableEvents)) {
            val newEvents = eventRevertPolicy.remove(event, entity.revertableEvents)
            reversedReducer.reduce(entity, event).withRevertableEvents(newEvents)
        } else {
            entity
        }
    }
}
