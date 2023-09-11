package com.rarible.core.reduce.blockchain

import com.rarible.core.reduce.model.ReduceSnapshot
import com.rarible.core.reduce.service.SnapshotStrategy
import com.rarible.core.reduce.service.SnapshotStrategy.Context
import java.lang.IllegalArgumentException
import java.util.concurrent.ConcurrentLinkedDeque
import java.util.concurrent.atomic.AtomicReference

open class BlockchainSnapshotStrategy<
        Snapshot : ReduceSnapshot<Data, Long, Key>,
        Data,
        Key>(
        private val numberOfBlockConfirmations: Int
    ) : SnapshotStrategy<Snapshot, Long> {

    override fun context(initial: Snapshot): Context<Snapshot, Long> {
        return object : Context<Snapshot, Long> {
            private val linkedDeque = ConcurrentLinkedDeque<Snapshot>()
            private val previousMark = AtomicReference<Long>(initial.mark)

            override fun validate(mark: Long) {
                if (previousMark.get() > mark) {
                    throw IllegalArgumentException(
                        "Previous mark $previousMark is greater than current mark $mark"
                    )
                }
            }

            override fun push(snapshot: Snapshot) {
                // This two operations allowed to be executed without synchronized as all thread confined
                linkedDeque.removeIf { it.mark == snapshot.mark }
                linkedDeque.push(snapshot)

                while (linkedDeque.size > numberOfBlockConfirmations) {
                    linkedDeque.removeLast()
                }
                previousMark.set(snapshot.mark)
            }

            override fun needSave(): Boolean {
                return if (linkedDeque.isNotEmpty()) test(linkedDeque.last, linkedDeque.first) else false
            }

            // start from the first item in the deque and find candidate for snapshot
            override fun next(): Snapshot {
                if (linkedDeque.isEmpty()) {
                    throw IllegalStateException("No snapshot elements to return")
                }
                return linkedDeque.find { test(it, linkedDeque.first) } ?: linkedDeque.last
            }

            private fun test(prev: Snapshot, next: Snapshot): Boolean {
                return next.mark - prev.mark >= numberOfBlockConfirmations - 1
            }
        }
    }
}
