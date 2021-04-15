package com.rarible.core.cache

import org.springframework.data.annotation.Id
import org.springframework.data.annotation.Version
import org.springframework.data.mongodb.core.mapping.Document
import java.util.*

@Document
data class Cache(
    @Id
    val id: String,
    val data: Any?,
    val updateDate: Date,
    @Version
    val version: Long? = null
) {
    fun canBeUsed(maxAge: Long): Boolean {
        return updateDate.time >= System.currentTimeMillis() - maxAge
    }
}