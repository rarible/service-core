package com.rarible.core.common

import java.util.SortedSet

object RangeUtils {

    fun splitIntoRanges(numbers: SortedSet<Long>, range: Int): List<Long> {
        if (numbers.isEmpty()) {
            return emptyList()
        }
        val froms = mutableListOf<Long>()
        var remains = numbers
        do {
            val from = remains.first()
            froms.add(from)
            val to = from + range - 1
            remains = remains.tailSet(to)
        } while (!remains.isEmpty())
        return froms
    }
}
