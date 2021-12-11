package com.rarible.core.entity.reducer.service

interface EventRevertPolicy<Event> {
    /**
     * Policy how to add new block to event list
     */
    fun add(event: Event, events: List<Event>): List<Event>

    /**
     * Policy how to subtract new block to event list
     */
    fun remove(event: Event, events: List<Event>): List<Event>

    /**
     * Check if we have already applied an income event
     */
    fun wasApplied(event: Event, events: List<Event>): Boolean
}
