package com.rarible.core.entity.reducer.service

import com.rarible.core.entity.reducer.model.Identifiable
import com.rarible.core.entity.reducer.service.ReduceService.EntityWithEvent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.flow

/**
 * Service used to calculate state of the entities by events from the database.
 * Events should be ordered by entity id (so events for one entity are collected together)
 * Service should be used in the long-running Task which updates state of entities
 */
open class StreamFullReduceService<Id, Event, E : Identifiable<Id>>(
    private val entityService: EntityService<Id, E, Event>,
    private val entityIdService: EntityIdService<Event, Id>,
    private val templateProvider: EntityTemplateProvider<Id, E>,
    private val reducer: Reducer<Event, E>,
    private val throttleSaveToDb: Int
) : ReduceService<Id, Event, E> {

    /**
     * Checking current entity with the entity after reducing before persisting into db
     */
    protected open fun isChanged(current: E?, result: E?): Boolean {
        return true
    }

    override fun reduceWithEvents(events: Flow<Event>): Flow<EntityWithEvent<E, Event>> = flow {
        var originalEntity: E? = null
        var eventsForEntity = 0
        var currentResult: EntityWithEvent<E, Event>? = null
        events.collect { event ->
            val id = entityIdService.getEntityId(event)
            val prevResult = currentResult
            // `prevResult.entity.id != id` means that we are reading next balance in the flow
            val currentEntity = if (prevResult == null || prevResult.entity.id != id) {
                if (prevResult != null) {
                    // for full reduce we don't need to specify event triggered the update - it is not actual
                    updateAndEmit(originalEntity, prevResult)
                }
                originalEntity = entityService.get(id)
                eventsForEntity = 0
                templateProvider.getEntityTemplate(id, originalEntity?.version) withEvent event
            } else {
                eventsForEntity += 1
                if (eventsForEntity == throttleSaveToDb) {
                    updateAndEmit(originalEntity, prevResult)
                    eventsForEntity = 0
                }
                prevResult
            }
            currentResult = reducer.reduce(currentEntity.entity, event) withEvent event
        }
        val prevResult = currentResult
        updateAndEmit(originalEntity, prevResult)
    }

    private suspend fun FlowCollector<EntityWithEvent<E, Event>>.updateAndEmit(original: E?, result: EntityWithEvent<E, Event>?) {
        if (result != null) {
            val emittedResult = if (isChanged(original, result.entity)) {
                entityService.update(result.entity) withEvent result.event
            } else result
            emit(emittedResult)
        }
    }

    private infix fun E.withEvent(event: Event) = EntityWithEvent(this, event)
}
