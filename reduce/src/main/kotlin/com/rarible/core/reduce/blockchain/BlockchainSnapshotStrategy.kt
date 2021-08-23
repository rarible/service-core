package com.rarible.core.reduce.blockchain

import com.rarible.core.reduce.model.DataKey
import com.rarible.core.reduce.model.ReduceSnapshot
import com.rarible.core.reduce.service.SnapshotStrategy
import com.rarible.core.reduce.service.SnapshotStrategy.Ctx
import java.lang.IllegalArgumentException
import java.util.concurrent.ConcurrentLinkedDeque
import java.util.concurrent.atomic.AtomicReference

open class BlockchainSnapshotStrategy<
        Snapshot : ReduceSnapshot<Data, Long, Key>,
        Data,
        Key : DataKey>(
    private val eventsCountBeforeNextSnapshot: Int
) : SnapshotStrategy<Snapshot, Long> {

    override fun ctx(initial: Snapshot): Ctx<Snapshot, Long> {
        return object : Ctx<Snapshot, Long> {
            private val linkedDeque = ConcurrentLinkedDeque<Snapshot>()
            private val previousMark = AtomicReference<Long>(initial.mark)
            override fun validate(currentMark: Long) {
                if (previousMark.get() > currentMark) {
                    throw IllegalArgumentException(
                        "Previous mark $previousMark is greater than current mark $currentMark"
                    )
                }
            }

            override fun push(snapshot: Snapshot) {
                linkedDeque.removeIf { it.mark == snapshot.mark }
                linkedDeque.push(snapshot)
                while (linkedDeque.size > eventsCountBeforeNextSnapshot) {
                    linkedDeque.removeLast()
                }
                previousMark.set(snapshot.mark)
            }

            override fun needSave(): Boolean = test(linkedDeque.last, linkedDeque.first)

            // start from the first item in the deque and find candidate for snapshot
            override fun next(): Snapshot = linkedDeque.find { test(it, linkedDeque.first) } ?: linkedDeque.last

            private fun test(prev: Snapshot, next: Snapshot): Boolean {
                return next.mark - prev.mark >= eventsCountBeforeNextSnapshot - 1
            }
        }
    }
}
