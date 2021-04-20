package com.rarible.core.mongowatcher

import org.springframework.data.mongodb.core.ReactiveMongoOperations
import java.util.*
import kotlin.collections.ArrayList

class MockChangeHandler(mongo: ReactiveMongoOperations) : MongoChangeHandler("table", mongo) {
    val events: MutableList<MongoEvent> = Collections.synchronizedList(ArrayList<MongoEvent>())

    override suspend fun onEvent(mongoEvent: MongoEvent) {
        events.add(mongoEvent)
    }
}