package com.rarible.core.entity.reducer.service.model

import com.rarible.core.entity.reducer.model.RevertableEntity

data class TestEntity(
    override val events: List<EntityEvent>,
    val intValue: Int = 0,
    override val id: Long = 0
) : RevertableEntity<Long, EntityEvent, TestEntity> {

    override fun withEvents(events: List<EntityEvent>): TestEntity {
        return copy(events = events)
    }
}
