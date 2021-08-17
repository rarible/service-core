package com.rarible.core.reduce.service

interface SnapshotStrategy<Snapshot, Mark> {
    fun ctx(initial: Snapshot): Ctx<Snapshot, Mark>

    interface Ctx<Snapshot, Mark> {
        fun push(snapshot: Snapshot)
        fun needSave(): Boolean
        fun validate(mark: Mark)
        fun last(): Snapshot
    }
}
