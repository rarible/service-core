package com.rarible.core.mongo.converters

import com.rarible.core.mongo.AbstractIntegrationTest
import org.bson.BsonUndefined
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update
import com.rarible.core.mongo.domain.BsonTest

class BsonUndefinedTest : AbstractIntegrationTest() {
    @Test
    fun loadUndefined() {
        val saved = mongo.save(BsonTest(value = "some value")).block()!!
        mongo.updateFirst(Query(), Update.update("value", BsonUndefined()), BsonTest::class.java).block()
        val read = mongo.findById(saved.id, BsonTest::class.java).block()!!
        assertNull(read.value)
    }
}
