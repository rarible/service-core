package com.rarible.core.mongo.template

import com.mongodb.reactivestreams.client.FindPublisher
import com.rarible.core.mongo.configuration.MongoProperties
import com.rarible.core.mongo.metrics.RaribleMongoMetrics
import org.bson.Document
import org.springframework.data.mongodb.ReactiveMongoDatabaseFactory
import org.springframework.data.mongodb.core.FindPublisherPreparer
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.convert.MongoConverter
import org.springframework.data.mongodb.core.convert.MongoWriter
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.Duration
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong

class RaribleReactiveMongoTemplate(
    reactiveMongoDatabaseFactory: ReactiveMongoDatabaseFactory,
    converter: MongoConverter,
    private val properties: MongoProperties,
    private val metrics: RaribleMongoMetrics,
) : ReactiveMongoTemplate(reactiveMongoDatabaseFactory, converter) {

    override fun <T> doSave(
        collectionName: String,
        objectToSave: T,
        writer: MongoWriter<Any>
    ): Mono<T> {
        return wrapQueryWithMetric(
            collection = collectionName,
            publisher = super.doSave(collectionName, objectToSave, writer),
            measure = metrics::saveLatency
        )
    }

    override fun <T> doFindOne(
        collectionName: String,
        query: Document,
        fields: Document?,
        entityClass: Class<T>,
        preparer: FindPublisherPreparer
    ): Mono<T> {
        return wrapQueryWithMetric(
            collection = collectionName,
            publisher = super.doFindOne(collectionName, query, fields, entityClass, preparer),
            measure = metrics::findOneLatency
        )
    }

    override fun <T : Any?> doFind(
        collectionName: String?,
        query: Document?,
        fields: Document?,
        entityClass: Class<T>?,
        preparer: FindPublisherPreparer?
    ): Flux<T> {
        return wrapQueryWithMetric(
            collection = collectionName,
            publisher = super.doFind(collectionName, query, fields, entityClass, MaxTimeAwarePreparer(preparer)),
            measure = metrics::findLatency
        )
    }

    inner class MaxTimeAwarePreparer(private val delegate: FindPublisherPreparer?) : FindPublisherPreparer {
        override fun prepare(findPublisher: FindPublisher<Document>): FindPublisher<Document> {
            val publisher = if (properties.maxTime != null) {
                findPublisher.maxTime(properties.maxTime.toMillis(), TimeUnit.MILLISECONDS)
            } else {
                findPublisher
            }
            return delegate?.prepare(publisher) ?: publisher
        }
    }

    private fun <T> wrapQueryWithMetric(
        collection: String?,
        publisher: Mono<T>,
        measure: (String, Duration) -> Unit
    ): Mono<T> {
        return if (properties.enableMetrics) {
            val start = AtomicLong()
            publisher
                .doOnSubscribe { start(start) }
                .doOnEach { measure(collection, start, measure) }
        } else {
            publisher
        }
    }

    private fun <T> wrapQueryWithMetric(
        collection: String?,
        publisher: Flux<T>,
        measure: (String, Duration) -> Unit
    ): Flux<T> {
        return if (properties.enableMetrics) {
            val start = AtomicLong()
            publisher
                .doOnSubscribe { start(start) }
                .doOnEach { measure(collection, start, measure) }
        } else {
            publisher
        }
    }

    private fun start(start: AtomicLong) {
        start.set(System.currentTimeMillis())
    }

    private fun measure(
        collection: String?,
        start: AtomicLong,
        measure: (String, Duration) -> Unit
    ) {
        if (start.get() > 0) {
            val stop = System.currentTimeMillis()
            val duration = Duration.ofMillis(stop - start.get())
            measure(collection ?: "unknown", duration)
            // reset start time to measure only first arrival document
            start.set(0)
        }
    }
}
