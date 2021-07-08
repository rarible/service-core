package com.rarible.core.reduce.model

abstract class ReduceSnapshot<Data, Mark : Comparable<Mark>> {
    abstract val data: Data
    abstract val mark: Mark
}