package com.rarible.core.reduce.queue

import com.rarible.core.reduce.model.ReduceSnapshot
import java.util.concurrent.ConcurrentLinkedDeque

internal class LimitedSnapshotQueue<Snapshot : ReduceSnapshot<Data, Mark, Key>, Data, Mark : Comparable<Mark>, Key>(
    private val limit: Int
) {
    init {
        require(limit > 0) { "Limit must be positive" }
    }

    private val concurrentLinkedDeque = ConcurrentLinkedDeque<Snapshot>()


    fun push(snapshot: Snapshot) {
        // This two operations allowed to execute without synchronized as all thread confined
        run {
            concurrentLinkedDeque.removeIf { it.mark == snapshot.mark }
            concurrentLinkedDeque.push(snapshot)
        }

        while (concurrentLinkedDeque.size > limit) {
            concurrentLinkedDeque.removeLast()
        }
    }

    fun getSnapshotList(): List<Snapshot> {
        return concurrentLinkedDeque.toList()
    }
}
