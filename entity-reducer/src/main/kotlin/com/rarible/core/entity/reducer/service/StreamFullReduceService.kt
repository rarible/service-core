package com.rarible.core.entity.reducer.service

import com.rarible.core.entity.reducer.model.Identifiable
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
    private val reducer: Reducer<Event, E>
) : ReduceService<Id, Event, E> {

    /**
     * Checking current entity with the entity after reducing before persisting into db
     */
    protected open fun isChanged(current: E?, result: E?): Boolean {
        return true
    }

    override fun reduce(events: Flow<Event>): Flow<E> = flow {
        var current: E? = null
        var entity: E? = null
        events.collect { event ->
            val id = entityIdService.getEntityId(event)
            val prevEntity = entity
            // `prevEntity.id != id` means that we are reading next balance in the flow
            val currentEntity = if (prevEntity == null || prevEntity.id != id) {
                if (prevEntity != null) {
                    // for full reduce we don't need to specify event triggered the update - it is not actual
                    checkAndEmit(current, prevEntity)
                }
                current = entityService.get(id)
                templateProvider.getEntityTemplate(id, current?.version)
            } else {
                prevEntity
            }
            entity = reducer.reduce(currentEntity, event)
        }
        val lastEntity = entity
        checkAndEmit(current, lastEntity)
    }

    private suspend fun FlowCollector<E>.checkAndEmit(current: E?, result: E?) {
        if (result != null) {
            val emittedEntity = if (isChanged(current, result)) {
                entityService.update(result)
            } else result
            emit(emittedEntity)
        }
    }
}
