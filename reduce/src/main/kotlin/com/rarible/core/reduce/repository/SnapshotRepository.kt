package com.rarible.core.reduce.repository

import com.rarible.core.reduce.model.DataKey
import com.rarible.core.reduce.model.ReduceSnapshot

interface SnapshotRepository<Mark : Comparable<Mark>, Data, Key : DataKey> {
    suspend fun get(key: Key): ReduceSnapshot<Data, Mark>?

    suspend fun save(snapshot: ReduceSnapshot<Data, Mark>)
 }