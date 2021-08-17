package com.rarible.core.reduce.service.reducer

import com.rarible.core.reduce.service.SnapshotStrategy
import com.rarible.core.reduce.service.SnapshotStrategy.Ctx
import com.rarible.core.reduce.service.model.AccountReduceSnapshot
import java.lang.IllegalArgumentException
import java.util.concurrent.ConcurrentLinkedDeque
import java.util.concurrent.atomic.AtomicReference

class AccountSnapshotStrategy(
    private val eventsCountBeforeNextSnapshot: Int
) : SnapshotStrategy<AccountReduceSnapshot, Long> {

    override fun ctx(initial: AccountReduceSnapshot): Ctx<AccountReduceSnapshot, Long> {
        return object: Ctx<AccountReduceSnapshot, Long> {
            private val concurrentLinkedDeque = ConcurrentLinkedDeque<AccountReduceSnapshot>()
            private val previousMark = AtomicReference<Long>(initial.mark)
            override fun validate(currentMark: Long) {
                if (previousMark.get() > currentMark) {
                    throw IllegalArgumentException(
                        "Previous mark $previousMark is greater than current mark $currentMark"
                    )
                }
            }
            override fun push(snapshot: AccountReduceSnapshot) {
                concurrentLinkedDeque.removeIf { it.mark == snapshot.mark }
                concurrentLinkedDeque.push(snapshot)
                while (concurrentLinkedDeque.size > eventsCountBeforeNextSnapshot) {
                    concurrentLinkedDeque.removeLast()
                }
                previousMark.set(snapshot.mark)
            }
            override fun needSave(): Boolean {
                return concurrentLinkedDeque.size >= eventsCountBeforeNextSnapshot
            }
            override fun last(): AccountReduceSnapshot {
                return concurrentLinkedDeque.last()
            }
        }
    }
}