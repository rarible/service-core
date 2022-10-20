package com.rarible.core.entity.reducer.service.model

import com.rarible.core.entity.reducer.service.LogStatus
import com.rarible.core.test.data.randomAddress
import com.rarible.core.test.data.randomInt
import com.rarible.core.test.data.randomLong
import com.rarible.core.test.data.randomString
import com.rarible.core.test.data.randomWord
import io.daonomic.rpc.domain.Word

data class ItemEvent(
    val status: LogStatus,
    val blockNumber: Long?,
    val logIndex: Int?,
    val minorLogIndex: Int,
    val transactionHash: String,
    val address: scalether.domain.Address,
    val topic: Word,
) : Comparable<ItemEvent> {
    override fun compareTo(other: ItemEvent): Int {
        val o1 = this
        return when (o1.status) {
            LogStatus.CONFIRMED, LogStatus.REVERTED -> {
                require(status == LogStatus.CONFIRMED || status == LogStatus.REVERTED) {
                    "Can't compare $o1 and $other"
                }
                confirmBlockComparator.compare(o1, other)
            }
            LogStatus.PENDING, LogStatus.INACTIVE, LogStatus.DROPPED -> {
                if (other.status == LogStatus.CONFIRMED) {
                    eventKeyComparator.compare(o1, other)
                } else {
                    require(
                        other.status == LogStatus.PENDING
                            || other.status == LogStatus.INACTIVE
                            || other.status == LogStatus.DROPPED
                    ) {
                        "Can't compare $o1 and $other"
                    }
                    pendingBlockComparator.compare(o1, other)
                }
            }
        }
    }

    private companion object {
        val confirmBlockComparator: Comparator<ItemEvent> = Comparator
            .comparingLong<ItemEvent> { requireNotNull(it.blockNumber) }
            .thenComparingInt { requireNotNull(it.logIndex) }
            .thenComparingInt { it.minorLogIndex }

        val pendingBlockComparator: Comparator<ItemEvent> = Comparator
            .comparing<ItemEvent, String>({ it.transactionHash }, { t1, t2 -> t1.compareTo(t2) })
            .thenComparing({ it.address.toString() }, { a1, a2 -> a1.compareTo(a2) })
            .thenComparing({ it.topic.toString() }, { a1, a2 -> a1.compareTo(a2) })
            .thenComparingInt { it.minorLogIndex }

        val eventKeyComparator: Comparator<ItemEvent> = Comparator
            .comparing<ItemEvent, String>({ it.transactionHash }, { t1, t2 -> t1.compareTo(t2) })
            .thenComparing({ it.address.toString() }, { a1, a2 -> a1.compareTo(a2) })
            .thenComparing({ it.topic.toString() }, { a1, a2 -> a1.compareTo(a2) })
    }
}

fun createRandomItemEvent(): ItemEvent = ItemEvent(
    status = LogStatus.values().random(),
    blockNumber = randomLong(),
    logIndex = randomInt(),
    minorLogIndex = randomInt(),
    transactionHash = randomString(),
    address = randomAddress(),
    topic = Word.apply(randomWord())
)