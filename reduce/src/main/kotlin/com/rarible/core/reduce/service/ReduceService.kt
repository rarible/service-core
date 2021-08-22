package com.rarible.core.reduce.service

import com.rarible.core.common.retryOptimisticLock
import com.rarible.core.reduce.model.DataKey
import com.rarible.core.reduce.model.ReduceEvent
import com.rarible.core.reduce.model.ReduceSnapshot
import com.rarible.core.reduce.repository.DataRepository
import com.rarible.core.reduce.repository.ReduceEventRepository
import com.rarible.core.reduce.repository.SnapshotRepository
import kotlinx.coroutines.flow.fold
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactor.asFlux
import kotlinx.coroutines.reactor.mono
import org.slf4j.LoggerFactory
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toFlux
import java.util.*

@Suppress("MemberVisibilityCanBePrivate")
class ReduceService<
        Event : ReduceEvent<Mark>,
        Snapshot : ReduceSnapshot<Data, Mark, Key>,
        Mark : Comparable<Mark>,
        Data,
        Key : DataKey>(
    private val reducer: Reducer<Event, Snapshot, Mark, Data, Key>,
    private val eventRepository: ReduceEventRepository<Event, Mark, Key>,
    private val snapshotRepository: SnapshotRepository<Snapshot, Data, Mark, Key>,
    private val dataRepository: DataRepository<Data>,
    private val snapshotStrategy: SnapshotStrategy<Snapshot, Mark>
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    suspend fun onEvents(events: List<Event>) {
        if (events.isEmpty()) return

        val minMark: Mark = events.minBy { it.mark }?.mark ?: error("Events array can't be empty")

        events.toFlux()
            .map { event -> reducer.getDataKeyFromEvent(event) }
            .distinct()
            .flatMap { update(it, minMark) }
            .then()
            .awaitFirstOrNull()
    }

    fun update(key: Key?, minMark: Mark): Flux<Key> {
        return getSnapshot(key, minMark)
            .flatMapMany { optionalSnapshot ->
                val snapshot = optionalSnapshot.orElse(null)

                eventRepository.getEvents(key, snapshot?.mark)
                    .asFlux()
                    .windowUntilChanged { event -> reducer.getDataKeyFromEvent(event) }
                    .concatMap {
                        it.switchOnFirst { first, events ->
                            val firstEvent = first.get()

                            if (firstEvent != null) {
                                val targetKey = reducer.getDataKeyFromEvent(firstEvent)

                                val initial = snapshot ?: reducer.getInitialData(targetKey)

                                updateData(initial, events)
                                    .retryOptimisticLock()
                                    .thenReturn(targetKey)
                            } else {
                                Mono.empty()
                            }
                        }
                    }
            }
    }

    private fun updateData(initialSnapshot: Snapshot, events: Flux<Event>): Mono<Key> = mono {
        val key = initialSnapshot.id
        var ctx = snapshotStrategy.ctx(initialSnapshot)

        logger.info("Started processing $key, startMark=${initialSnapshot.mark}")
        val reducedSnapshot = events
            .asFlow()
            .fold(initialSnapshot) { initial, event ->
                ctx.validate(event.mark)
                val intermediateSnapshot = reducer.reduce(initial, event)
                ctx.push(intermediateSnapshot)
                intermediateSnapshot
            }

        if (reducedSnapshot != initialSnapshot) {
            dataRepository.saveReduceResult(reducedSnapshot.data)
            logger.info("Save new reduce data for $key")

            if (ctx.needSave()) {
                snapshotRepository.save(ctx.next())
                logger.info("Save new snapshot for $key")
            }
        }
        logger.info("Finish for $key, endMark=${reducedSnapshot.mark}")
        key
    }

    private fun getSnapshot(key: Key?, minMark: Mark): Mono<Optional<Snapshot>> = mono {
        key
            ?.let { snapshotRepository.get(it) }
            ?.takeIf { minMark > it.mark }
            ?.let { Optional.of(it) }
            ?: Optional.empty()
    }
}
