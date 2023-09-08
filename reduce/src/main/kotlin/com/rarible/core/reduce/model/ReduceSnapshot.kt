package com.rarible.core.reduce.model

abstract class ReduceSnapshot<Data, Mark : Comparable<Mark>, Key> {
    abstract val id: Key
    abstract val data: Data
    abstract val mark: Mark
}
