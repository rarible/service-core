package com.rarible.core.entity.reducer.service

import com.rarible.core.entity.reducer.model.RevertableEntity

class EntityReducer<Id, Event, E : RevertableEntity<Id, Event, E>>(
    private val eventRevertPolicy: EventRevertPolicy<Event>,
    private val reducer: Reducer<Event, E>
) : Reducer<Event, E> {

    override suspend fun reduce(entity: E, event: Event): E {
        return if (eventRevertPolicy.wasApplied(event, entity.revertableEvents)) {
            entity
        } else {
            val newEvents = eventRevertPolicy.add(event, entity.revertableEvents)
            reducer.reduce(entity, event).withRevertableEvents(newEvents)
        }
    }
}
