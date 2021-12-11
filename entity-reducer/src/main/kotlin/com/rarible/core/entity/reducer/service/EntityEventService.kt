package com.rarible.core.entity.reducer.service

interface EntityEventService<Event, Id> {
    fun getEntityId(event: Event): Id
}
