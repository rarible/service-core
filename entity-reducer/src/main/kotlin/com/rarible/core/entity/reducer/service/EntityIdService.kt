package com.rarible.core.entity.reducer.service

interface EntityIdService<Event, Id> {
    fun getEntityId(event: Event): Id
}
