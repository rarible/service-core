package com.rarible.core.common

fun <K : Any> Grouping<*, K>.keySet(): MutableSet<K> =
    aggregateTo(HashMap<K, K>()) { key, _, _, _ -> key }.keys

fun <T, K> Grouping<T, K>.eachFirst(): Map<K, T> =
    aggregate { _, firstInGroup: T?, element, _ ->
        firstInGroup ?: element
    }

fun <T, K> Grouping<T, K>.eachMin(comparator: Comparator<T>): Map<K, T> =
    aggregate { _, min: T?, element, isFirst ->
        if (isFirst || comparator.compare(min, element) > 0) {
            element
        } else {
            min!!
        }
    }

fun <T : Comparable<T>, K> Grouping<T, K>.eachMin(): Map<K, T> = eachMin(naturalOrder())
