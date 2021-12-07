package com.rarible.core.entity.reducer.service.service

import com.rarible.core.entity.reducer.service.Reducer
import com.rarible.core.entity.reducer.service.model.EntityEvent
import com.rarible.core.entity.reducer.service.model.TestEntity

class TestEntityReducer : Reducer<EntityEvent, TestEntity> {
    override suspend fun reduce(entity: TestEntity, event: EntityEvent): TestEntity {
        return entity.copy(intValue = entity.intValue + event.intValue)
    }
}
