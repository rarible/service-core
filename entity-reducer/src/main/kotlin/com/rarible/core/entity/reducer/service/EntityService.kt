package com.rarible.core.entity.reducer.service

interface EntityService<Id, E> {
    suspend fun get(id: Id): E?

    suspend fun update(entity: E): E
}
