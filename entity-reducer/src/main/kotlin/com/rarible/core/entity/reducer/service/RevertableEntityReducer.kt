package com.rarible.core.entity.reducer.service

import com.rarible.core.entity.reducer.model.RevertableEntity

class RevertableEntityReducer<Id, Event : Comparable<Event>, E : RevertableEntity<Id, Event, E>>(
    private val eventRevertService: EventRevertService<Event>,
    private val reducer: Reducer<Event, E>
) : Reducer<Event, E> {

    override suspend fun reduce(entity: E, event: Event): E {
        val lastEvent = entity.events.lastOrNull()
        // We have strict event order, so all new events must be greater than last in entity events list
        return if (lastEvent == null || event > lastEvent) {
            val newEvents = (entity.events + event)
                .filter { eventRevertService.canBeReverted(event, it) }
            reducer.reduce(entity, event).withEvents(newEvents)
        } else {
            entity
        }
    }
}
