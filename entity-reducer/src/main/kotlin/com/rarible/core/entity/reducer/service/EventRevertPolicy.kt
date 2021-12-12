package com.rarible.core.entity.reducer.service

interface EventRevertPolicy<Event> {
    /**
     * Reduce a new income event with list of applied events
     */
    fun reduce(events: List<Event>, event: Event): List<Event>

    /**
     * Check if we have already applied an income event
     */
    fun wasApplied(events: List<Event>, event: Event): Boolean
}
