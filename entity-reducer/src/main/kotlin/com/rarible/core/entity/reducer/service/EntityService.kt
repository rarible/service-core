package com.rarible.core.entity.reducer.service

interface EntityService<Id, E, Event> {

    suspend fun get(id: Id): E?

    /**
     * Update existing entity. Optionally, event triggered this update might be provided.
     * Event might be not specified, if update triggered by some internal logic -
     * for example, manual re-reduce of all entity's events
     */
    suspend fun update(entity: E, event: Event? = null): E
}
