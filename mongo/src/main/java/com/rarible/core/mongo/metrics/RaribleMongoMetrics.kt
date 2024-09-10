package com.rarible.core.mongo.metrics

import com.rarible.core.telemetry.metrics.AbstractMetrics
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Tag
import java.time.Duration

class RaribleMongoMetrics(
    meterRegistry: MeterRegistry
) : AbstractMetrics(meterRegistry) {

    fun saveLatency(
        collection: String,
        latency: Duration
    ) {
        record(
            MONGO_QUERY,
            latency,
            MONGO_DELAY_OBJECTIVES,
            collection(collection),
            operation("save")
        )
    }

    fun findLatency(
        collection: String,
        latency: Duration
    ) {
        record(
            MONGO_QUERY,
            latency,
            MONGO_DELAY_OBJECTIVES,
            collection(collection),
            operation("find")
        )
    }

    fun findOneLatency(
        collection: String,
        latency: Duration
    ) {
        record(
            MONGO_QUERY,
            latency,
            MONGO_DELAY_OBJECTIVES,
            collection(collection),
            operation("find-one")
        )
    }

    private fun collection(collection: String): Tag {
        return tag("collection", collection)
    }

    private fun operation(operation: String): Tag {
        return tag("operation", operation)
    }

    private companion object {
        const val MONGO_QUERY = "rarible_core_mongo_query"

        val MONGO_DELAY_OBJECTIVES = listOf(
            Duration.ofSeconds(1),
            Duration.ofSeconds(3),
            Duration.ofSeconds(5),
            Duration.ofSeconds(12),
            Duration.ofSeconds(15),
            Duration.ofSeconds(20),
            Duration.ofSeconds(30),
            Duration.ofMinutes(1),
            Duration.ofMinutes(2)
        )
    }
}
