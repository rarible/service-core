package com.rarible.core.reduce.service

interface SnapshotStrategy<Snapshot, Mark> {
    fun context(initial: Snapshot): Context<Snapshot, Mark>

    interface Context<Snapshot, Mark> {
        fun push(snapshot: Snapshot)

        fun needSave(): Boolean

        fun validate(mark: Mark)

        fun next(): Snapshot
    }
}
