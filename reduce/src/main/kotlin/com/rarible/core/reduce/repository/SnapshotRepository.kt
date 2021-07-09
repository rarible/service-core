package com.rarible.core.reduce.repository

import com.rarible.core.reduce.model.ReduceSnapshot

interface SnapshotRepository<Snapshot: ReduceSnapshot<Data, Mark>, Data, Mark : Comparable<Mark>, Key> {
    suspend fun get(key: Key): Snapshot?

    suspend fun save(snapshot: Snapshot): Snapshot
 }