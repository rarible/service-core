package com.rarible.core.entity.reducer.service

interface EntityEventService<Event : Comparable<Event>, Id> {
    fun getEntityId(event: Event): Id
}
