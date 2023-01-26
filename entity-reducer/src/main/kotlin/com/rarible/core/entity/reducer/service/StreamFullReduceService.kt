package com.rarible.core.entity.reducer.service

import com.rarible.core.entity.reducer.model.Identifiable
import kotlinx.coroutines.flow.Flow
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

    override fun reduce(events: Flow<Event>): Flow<E> = flow {
        var entity: E? = null
        events.collect { event ->
            val id = entityIdService.getEntityId(event)
            val prevEntity = entity
            val currentEntity = if (prevEntity == null || prevEntity.id != id) {
                if (prevEntity != null) {
                    // for full reduce we don't need to specify event triggered the update - it is not actual
                    emit(entityService.update(prevEntity))
                }
                val version = entityService.get(id)?.version
                templateProvider.getEntityTemplate(id, version)
            } else {
                prevEntity
            }
            entity = reducer.reduce(currentEntity, event)
        }
        val lastEntity = entity
        if (lastEntity != null) {
            emit(entityService.update(lastEntity))
        }
    }
}
