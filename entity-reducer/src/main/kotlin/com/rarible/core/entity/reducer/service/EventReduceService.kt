package com.rarible.core.entity.reducer.service

import com.rarible.core.common.asyncWithTraceId
import com.rarible.core.common.optimisticLockWithInitial
import com.rarible.core.entity.reducer.model.Identifiable
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.slf4j.LoggerFactory
import java.util.Optional

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

    private val logger = LoggerFactory.getLogger(javaClass)

    /**
     * Takes all events that need to be applied to the entities,
     * groups them by entity id and applies using batch to every entity
     */
    suspend fun reduceAll(events: List<Event>) {
        val start = System.currentTimeMillis()
        val grouped = events.groupBy { entityIdService.getEntityId(it) }
        val initial = entityService.getAll(grouped.keys).associateBy { it.id }
        val getSpent = System.currentTimeMillis() - start

        coroutineScope {
            grouped.map { (id, events) ->
                asyncWithTraceId(context = NonCancellable) {
                    reduce(id, Optional.ofNullable(initial[id]), events)
                }
            }.awaitAll()
        }

        val reduceSpent = System.currentTimeMillis() - start - getSpent
        // Debug information, don't want to spam with small batches report
        if (events.size > 10) {
            logger.info(
                "Reduced batch of {} events for {} entities, (get: {}ms , reduce: {}ms)",
                events.size, grouped.size, getSpent, reduceSpent
            )
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
    private suspend fun reduce(
        id: Id,
        initial: Optional<E>,
        events: List<Event>
    ): E {
        return optimisticLockWithInitial(
            initial = initial,
            optimisticExceptionHandler = { onOptimisticLockException(id, it) }
        ) {
            val entity = when {
                // Second try after optimistic lock exception - means entity exists
                it == null -> entityService.get(id) ?: getTemplate(id)
                // Initial is not null, but content is null - means entity doesn't exist, using template
                !it.isPresent -> getTemplate(id)
                // Otherwise - entity already exists, first try without previous OL exception
                else -> it.get()
            }

            val result = events.fold(entity) { e, event ->
                reducer.reduce(e, event)
            }

            if (isChanged(entity, result)) {
                entityService.update(result, events.lastOrNull())
            }
            result
        }
    }

    private fun onOptimisticLockException(id: Id, retry: Int) {
        logger.info("Optimistic lock exception caught for $id, retry $retry")
    }

    private fun getTemplate(id: Id): E {
        return templateProvider.getEntityTemplate(id, version = null)
    }
}
