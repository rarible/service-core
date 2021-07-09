package com.rarible.core.reduce.service

import com.rarible.core.reduce.model.DataKey
import com.rarible.core.reduce.model.ReduceEvent
import com.rarible.core.reduce.model.ReduceSnapshot
import kotlinx.coroutines.flow.Flow

interface Reducer<Event : ReduceEvent<Mark>, Snapshot : ReduceSnapshot<Data, Mark>, Mark : Comparable<Mark>, Data, Key : DataKey> {
    fun getDataKeyFromEvent(event: Event): Key

    fun getInitialData(key: Key): Data

    suspend fun reduce(initial: Data, events: Flow<Event>): Snapshot
}