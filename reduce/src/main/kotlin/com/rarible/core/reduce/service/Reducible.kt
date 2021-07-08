package com.rarible.core.reduce.service

import com.rarible.core.reduce.model.DataKey
import com.rarible.core.reduce.model.ReduceEvent
import com.rarible.core.reduce.model.ReduceSnapshot
import reactor.core.publisher.Flux

interface Reducible<Event : ReduceEvent<Mark>, Mark : Comparable<Mark>, Data, Key : DataKey> {
    fun getDataKeyFromEvent(event: Event): Key

    fun getInitialData(key: Key): Data

    fun reduce(initial: Data, events: Flux<Event>): Flux<ReduceSnapshot<Data, Mark>>
}