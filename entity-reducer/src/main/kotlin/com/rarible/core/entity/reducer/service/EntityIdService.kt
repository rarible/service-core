package com.rarible.core.entity.reducer.service

interface EntityIdService<E, Event, Id> {
    fun getEntityId(event: Event): Id
}
