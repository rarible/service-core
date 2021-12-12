package com.rarible.core.entity.reducer.model

/**
 * Entity that supports reverting events (for example when chain is reorganized)
 */
interface Entity<Id, Event, E : Entity<Id, Event, E>> : Identifiable<Id> {
    /**
     * These events will always be ordered in natural order
     */
    val revertableEvents: List<Event>

    /**
     * Copy and create entity with new event list
     */
    fun withRevertableEvents(events: List<Event>): E
}
