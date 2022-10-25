package com.rarible.core.mongo.converters

import com.fasterxml.jackson.databind.node.JsonNodeFactory
import com.rarible.core.mongo.AbstractIntegrationTest
import com.rarible.core.mongo.domain.JsonEntity
import org.bson.Document
import org.bson.types.ObjectId
import org.junit.jupiter.api.Test
import org.springframework.data.mongodb.core.findById
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update

class ObjectNodeConverterTest : AbstractIntegrationTest() {

    @Test
    fun saveLoad() {
        val obj = JsonNodeFactory.instance.objectNode()
            .put("test", "value")
        val saved = mongo.save(JsonEntity(ObjectId.get(), obj)).block()!!
        val found = mongo.findById<JsonEntity>(saved.id).block()
        print(found)
    }

    @Test
    fun string() {
        val obj = JsonNodeFactory.instance.objectNode()
            .put("test", "value")
        val saved = mongo.save(JsonEntity(ObjectId.get(), obj)).block()!!
        mongo.update(JsonEntity::class.java).matching(Query()).apply(Update.update("value", Document("test", "value2"))).all().block()
        val found = mongo.findById<JsonEntity>(saved.id).block()
        print(found)
    }
}
