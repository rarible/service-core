package com.rarible.core.entity.reducer.service

interface EntityIdService<E, Event, Id> {
    fun getEventEntityId(event: Event): Id
    fun getEntityId(entity: E): Id
}
