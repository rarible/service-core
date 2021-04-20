package com.rarible.core.mongowatcher

import org.springframework.data.mongodb.core.ReactiveMongoOperations
import kotlin.reflect.KClass

abstract class MongoEntityChangeHandler(
    mongo: ReactiveMongoOperations,
    entityType: KClass<*>
): MongoChangeHandler(mongo.getCollectionName(entityType.java), mongo)