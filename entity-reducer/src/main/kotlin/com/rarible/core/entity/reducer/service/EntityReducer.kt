package com.rarible.core.entity.reducer.service

import com.rarible.core.entity.reducer.model.Entity

class EntityReducer<Id, Event, E : Entity<Id, Event, E>>(
    private val eventApplyPolicy: EventApplyPolicy<Event>,
    private val reducer: Reducer<Event, E>
) : Reducer<Event, E> {

    override suspend fun reduce(entity: E, event: Event): E {
        return if (eventApplyPolicy.wasApplied(entity.revertableEvents, event)) {
            entity
        } else {
            val newEvents = eventApplyPolicy.reduce(entity.revertableEvents, event)
            reducer.reduce(entity, event).withRevertableEvents(newEvents)
        }
    }
}
