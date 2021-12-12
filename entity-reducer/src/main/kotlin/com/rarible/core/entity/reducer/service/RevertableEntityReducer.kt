package com.rarible.core.entity.reducer.service

import com.rarible.core.entity.reducer.model.RevertableEntity

class RevertableEntityReducer<Id, Event, E : RevertableEntity<Id, Event, E>>(
    private val eventRevertPolicy: EventRevertPolicy<Event>,
    private val reversedReducer: Reducer<Event, E>
) : Reducer<Event, E> {

    override suspend fun reduce(entity: E, event: Event): E {
        return if (eventRevertPolicy.wasApplied(entity.revertableEvents, event)) {
            val newEvents = eventRevertPolicy.reduce(entity.revertableEvents, event)
            reversedReducer.reduce(entity, event).withRevertableEvents(newEvents)
        } else {
            entity
        }
    }
}
