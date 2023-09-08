package com.rarible.core.mongo.double

import com.rarible.core.mongo.AbstractIntegrationTest
import com.rarible.core.mongo.domain.DoubleTest
import org.bson.types.Decimal128
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update

class Decimal128ToDoubleTest : AbstractIntegrationTest() {
    @Test
    fun convert() {
        val saved = mongo.save(DoubleTest(100.0)).block()!!
        mongo.updateFirst(Query(), Update().set("value", Decimal128(200)), DoubleTest::class.java).block()

        val read = mongo.findById(saved.id, DoubleTest::class.java).block()!!
        assertEquals(read.value, 200.toDouble())
    }
}
