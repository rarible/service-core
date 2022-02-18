package com.rarible.core.mongo.query

import org.springframework.beans.factory.annotation.Value
import org.springframework.data.mongodb.core.query.Query
import org.springframework.stereotype.Component

@Component
class QueryExtension(
    @Value("\${rarible.core.mongo.maxTime.fast:3000}") private val fast: Long,
    @Value("\${rarible.core.mongo.maxTime.medium:50000}") private val medium: Long,
    @Value("\${rarible.core.mongo.maxTime.slow:600000}") private val slow: Long
) {

    init {
        FAST = fast
        MEDIUM = medium
        SLOW = slow
    }

    companion object {
        var FAST: Long = 0
        var MEDIUM: Long = 0
        var SLOW: Long = 0
    }
}

fun Query.fast() = this.maxTimeMsec(QueryExtension.FAST)
fun Query.medium() = this.maxTimeMsec(QueryExtension.MEDIUM)
fun Query.slow() = this.maxTimeMsec(QueryExtension.SLOW)
