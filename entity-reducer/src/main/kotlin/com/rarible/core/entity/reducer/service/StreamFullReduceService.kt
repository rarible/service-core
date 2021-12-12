package com.rarible.core.entity.reducer.service

import com.rarible.core.entity.reducer.model.Identifiable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow

/**
 * Service used to calculate state of the entities by events from the database.
 * Events should be ordered by entity id (so events for one entity are collected together)
 * Service should be used in the long-running Task which updates state of entities
 */
open class StreamFullReduceService<Id, Event, E : Identifiable<Id>>(
    private val entityService: EntityService<Id, E>,
    private val entityIdService: EntityIdService<Event, Id>,
    private val templateProvider: EntityTemplateProvider<Id, E>,
    private val reducer: Reducer<Event, E>
) : ReduceService<Id, Event, E> {

    override suspend fun reduce(events: Flow<Event>): Flow<E> = flow {
        var entity: E? = null
        events.collect { event ->
            val id = entityIdService.getEntityId(event)
            val prevEntity = entity
            val currentEntity = if (prevEntity == null || prevEntity.id != id) {
                if (prevEntity != null) {
                    entityService.update(prevEntity)
                    emit(prevEntity)
                }
                templateProvider.getEntityTemplate(id)
            } else {
                prevEntity
            }
            entity = reducer.reduce(currentEntity, event)
        }
        val lastEntity = entity
        if (lastEntity != null) {
            entityService.update(lastEntity)
            emit(lastEntity)
        }
    }
}
