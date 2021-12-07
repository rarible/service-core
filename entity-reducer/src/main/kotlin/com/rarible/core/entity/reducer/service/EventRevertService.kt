package com.rarible.core.entity.reducer.service

interface EventRevertService<Event> {
    /**
     * Checks if this event can be reverted (it's pending or from latest n blocks)
     */
    fun canBeReverted(last: Event, current: Event): Boolean
}
