package com.rarible.core.entity.reducer.service

import com.rarible.core.common.optimisticLock
import com.rarible.core.entity.reducer.model.Identifiable
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

/**
 * Service to handle small amount of events for one entity
 * It takes a batch of events, loads entity, applies these events and saves entity to the database
 */
open class EventReduceService<Id, Event, E : Identifiable<Id>>(
    private val entityService: EntityService<Id, E, Event>,
    private val entityIdService: EntityIdService<Event, Id>,
    private val templateProvider: EntityTemplateProvider<Id, E>,
    private val reducer: Reducer<Event, E>
) {
    /**
     * Takes all events that need to be applied to the entities,
     * groups them by entity id and applies using batch to every entity
     */
    suspend fun reduceAll(event: List<Event>) {
        coroutineScope {
            event.groupBy { entityIdService.getEntityId(it) }.map { (id, events) ->
                async {
                    reduce(id, events)
                }
            }.awaitAll()
        }
    }

    /**
     * Checking current entity with the entity after reducing before persisting into db
     */
    protected open fun isChanged(current: E, result: E): Boolean {
        return true
    }

    /**
     * Takes batch of events that needs to be applied to one entity.
     * Gets entity by id, applies events and saves the entity
     */
    private suspend fun reduce(id: Id, events: List<Event>): E {
        return optimisticLock {
            val entity = entityService.get(id) ?: templateProvider.getEntityTemplate(id, version = null)

            val result = events.fold(entity) { e, event ->
                reducer.reduce(e, event)
            }

            if (isChanged(entity, result)) {
                entityService.update(result, events.lastOrNull())
            }
            result
        }
    }
}
