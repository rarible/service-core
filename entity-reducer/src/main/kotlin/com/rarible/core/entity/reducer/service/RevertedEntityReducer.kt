package com.rarible.core.entity.reducer.service

import com.rarible.core.entity.reducer.model.RevertableEntity

class RevertedEntityReducer<Id, Event, E : RevertableEntity<Id, Event, E>>(
    private val eventApplyPolicy: EventApplyPolicy<Event>,
    private val reversedReducer: Reducer<Event, E>
) : Reducer<Event, E> {

    override suspend fun reduce(entity: E, event: Event): E {
        return if (eventApplyPolicy.wasApplied(entity.revertableEvents, event)) {
            val newEvents = eventApplyPolicy.reduce(entity.revertableEvents, event)
            reversedReducer.reduce(entity, event).withRevertableEvents(newEvents)
        } else {
            entity
        }
    }
}
