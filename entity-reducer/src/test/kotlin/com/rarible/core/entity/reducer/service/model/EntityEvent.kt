package com.rarible.core.entity.reducer.service.model

data class EntityEvent(
    val block: Long,
    val intValue: Int = 0,
    val entityId: Long = 0
) : Comparable<EntityEvent> {
    override fun compareTo(other: EntityEvent): Int {
        return (block - other.block).toInt()
    }
}
