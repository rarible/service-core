package com.rarible.core.common

fun <T : Any> Sequence<T>.mergeSorted(other: Sequence<T>, comparator: Comparator<T>) = sequence<T> {
    val it1 = iterator()
    val it2 = other.iterator()
    var v1 = proceed(it1)
    var v2 = proceed(it2)
    while (v1 != null || v2 != null) {
        if (v1 == null) {
            yield(v2!!)
            v2 = proceed(it2)
        } else if (v2 == null) {
            yield(v1)
            v1 = proceed(it1)
        } else {
            when {
                comparator.compare(v1, v2) <= 0 -> {
                    yield(v1)
                    v1 = proceed(it1)
                }
                else -> {
                    yield(v2)
                    v2 = proceed(it2)
                }
            }
        }
    }
}

fun <T> Sequence<T>.mergeSorted(other: Sequence<T>) where T : Any, T : Comparable<T> =
    mergeSorted(other, Comparator.naturalOrder())

private fun <T> proceed(iterator: Iterator<T>) = if (iterator.hasNext()) iterator.next() else null

fun <T, K> Sequence<T>.sortedUnique(by: (T) -> K) = sequence<T> {
    var first = true
    var prev: K? = null
    forEach { element ->
        if (first) {
            first = false
            yield(element)
            prev = by(element)
        } else {
            val key = by(element)
            if (key != prev) {
                yield(element)
                prev = key
            }
        }
    }
}

fun <T> Sequence<T>.sortedUnique() = sortedUnique { it }
