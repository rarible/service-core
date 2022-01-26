package com.rarible.core.entity.reducer.service

import com.rarible.core.apm.withSpan
import com.rarible.core.common.optimisticLock
import com.rarible.core.entity.reducer.model.Identifiable

/**
 * Service to handle small amount of events for one entity
 * It takes a batch of events, loads entity, applies these events and saves entity to the database
 */
class EventReduceService<Id, Event, E : Identifiable<Id>>(
    private val entityService: EntityService<Id, E>,
    private val entityIdService: EntityIdService<Event, Id>,
    private val templateProvider: EntityTemplateProvider<Id, E>,
    private val reducer: Reducer<Event, E>
) {
    /**
     * Takes all events that need to be applied to the entities,
     * groups them by entity id and applies using batch to every entity
     */
    suspend fun reduceAll(event: List<Event>) {
        event
            .groupBy { entityIdService.getEntityId(it) }
            .forEach { (id, events) ->
                withSpan(name = "reduceEntity", labels = listOf("entityId" to id.toString(), "size" to events.size)) {
                    reduce(id, events)
                }
            }
    }

    /**
     * Takes batch of events that needs to be applied to one entity.
     * Gets entity by id, applies events and saves the entity
     */
    private suspend fun reduce(id: Id, events: List<Event>): E {
        return optimisticLock {
            val entity = withSpan(name = "get", labels = listOf("id" to id.toString())) { entityService.get(id) ?: templateProvider.getEntityTemplate(id) }
            val result = withSpan(name = "reduce", labels = listOf("id" to id.toString())) {
                events.fold(entity) { e, event ->
                    reducer.reduce(e, event)
                }
            }
            withSpan(name = "save", labels = listOf("id" to id.toString())) { entityService.update(result) }
            result
        }
    }
}
