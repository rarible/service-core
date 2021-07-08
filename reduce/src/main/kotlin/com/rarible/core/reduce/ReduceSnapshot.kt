package com.rarible.core.reduce

import com.rarible.core.common.retryOptimisticLock
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactor.mono
import org.slf4j.LoggerFactory
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toFlux

interface DataKey

data class ReduceSnapshot<Data, Mark : Comparable<Mark>>(
    val data: Data,
    val mark: Mark
)

interface SnapshotRepository<Mark : Comparable<Mark>, Data, Key : DataKey> {
    suspend fun get(key: Key): ReduceSnapshot<Data, Mark>?

    suspend fun save(snapshot: ReduceSnapshot<Data, Mark>)
 }

interface ReduceEventRepository<Event : ReduceEvent<Mark>, Mark : Comparable<Mark>, Key : DataKey> {
    fun getEvents(key: Key?, from: Mark?): Flux<Event>
}

interface DataRepository<Data> {
    suspend fun save(data: Data)
}

interface Reducible<Event : ReduceEvent<Mark>, Mark : Comparable<Mark>, Data, Key : DataKey> {
    fun getDataKeyFromEvent(event: Event): Key

    fun getInitialData(key: Key): Data

    fun reduce(initial: Data, events: Flux<Event>): Data
}


interface ReduceEvent<Mark : Comparable<Mark>> {
    val mark: Mark
}

class ReduceService<in Event : ReduceEvent<Mark>, Mark : Comparable<Mark>, Data, Key : DataKey>(
    private val reducible: Reducible<Event, Mark, Data, Key>,
    private val eventRepository: ReduceEventRepository<Event, Mark, Key>,
    private val snapshotRepository: SnapshotRepository<Mark, Data, Key>,
    private val dataRepository: DataRepository<Data>
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    suspend fun onEvents(events: List<Event>) {
        val context = ReduceContext(events)

        events.toFlux()
            .map { event -> reducible.getDataKeyFromEvent(event) }
            .distinct()
            .flatMap { update(it, context) }
            .awaitFirstOrNull()
    }

    private fun update(key: Key?, context: ReduceContext<Event, Mark>) = mono {
        val snapshot = key
            ?.let { snapshotRepository.get(it) }
            ?.takeIf { context.minMark > it.mark  }

        eventRepository.getEvents(key, snapshot?.mark)
            .windowUntilChanged { event -> reducible.getDataKeyFromEvent(event) }
            .concatMap {
                it.switchOnFirst { first, events ->
                    val firstEvent = first.get()

                    if (firstEvent != null) {
                        val targetKey = reducible.getDataKeyFromEvent(firstEvent)
                        logger.info("Started processing $targetKey")

                        val initial = snapshot?.data ?: reducible.getInitialData(targetKey)

                        updateData(initial, events)
                            .retryOptimisticLock()
                            .thenReturn(targetKey)
                    } else {
                        Mono.empty()
                    }
                }
            }
            .then()
    }

    private fun updateData(initial: Data, events: Flux<Event>) = mono {
        val data = reducible.reduce(initial, events)
        dataRepository.save(data)
    }

    private class ReduceContext<out Event : ReduceEvent<Mark>, Mark : Comparable<Mark>>(
        val events: List<Event>
    ) {
        val minMark: Mark = events.minBy { it.mark }?.mark ?: error("Events array can't be empty")
    }
}