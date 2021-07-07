package com.rarible.core.mongo.converters

import com.rarible.core.mongo.AbstractIntegrationTest
import org.bson.types.ObjectId
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.findById
import java.math.BigDecimal

class BigDecimalConverterTest : AbstractIntegrationTest() {
    @Test
    fun saveAndFoundSimpleBigDecimal() {
        val savingItem = WithBigDecimal(ObjectId.get(), BigDecimal("100.111"))
        val saved = mongo.save(savingItem).block()!!

        val found = mongo.findById<WithBigDecimal>(saved.id).block()

        assertEquals(saved, found)
    }

    @Test
    fun saveAndFoundSimpleVeryPrecision() {
        val savingItem = WithBigDecimal(ObjectId.get(), BigDecimal("12848.0862187168335294117647058823529"))
        val saved = mongo.save(savingItem).block()!!

        val found = mongo.findById<WithBigDecimal>(saved.id).block()!!

        assertEquals(found.value, BigDecimal("12848.08621871683352941176470588235"))
    }

    data class WithBigDecimal(
        @Id
        val id: ObjectId,
        val value: BigDecimal
    )
}
