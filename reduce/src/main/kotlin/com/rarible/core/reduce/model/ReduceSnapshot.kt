package com.rarible.core.reduce.model

data class ReduceSnapshot<Data, Mark : Comparable<Mark>>(
    val data: Data,
    val mark: Mark
)