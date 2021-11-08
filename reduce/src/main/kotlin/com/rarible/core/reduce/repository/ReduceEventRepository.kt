package com.rarible.core.reduce.repository

import com.rarible.core.reduce.model.ReduceEvent
import kotlinx.coroutines.flow.Flow

interface ReduceEventRepository<Event : ReduceEvent<Mark>, Mark : Comparable<Mark>, Key> {
    fun getEvents(key: Key?, after: Mark?): Flow<Event>
}
