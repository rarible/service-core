package com.rarible.core.mongo.template

import com.mongodb.ReadPreference
import com.mongodb.reactivestreams.client.FindPublisher
import com.mongodb.reactivestreams.client.MongoCollection
import com.mongodb.reactivestreams.client.MongoDatabase
import com.rarible.core.mongo.configuration.MongoProperties
import com.rarible.core.mongo.metrics.RaribleMongoMetrics
import org.bson.Document
import org.springframework.dao.support.PersistenceExceptionTranslator
import org.springframework.data.mongodb.ReactiveMongoDatabaseFactory
import org.springframework.data.mongodb.core.FindPublisherPreparer
import org.springframework.data.mongodb.core.ReactiveCollectionCallback
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.convert.MongoConverter
import org.springframework.data.mongodb.core.convert.MongoWriter
import org.springframework.util.Assert
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.util.context.ContextView
import java.time.Duration
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong
import java.util.function.Function

class RaribleReactiveMongoTemplate(
    reactiveMongoDatabaseFactory: ReactiveMongoDatabaseFactory,
    converter: MongoConverter,
    private val properties: MongoProperties,
    private val metrics: RaribleMongoMetrics,
    sessionStalenessSeconds: Int = 90,
    private val readReplicaEnabled: () -> Boolean = { false }
) : ReactiveMongoTemplate(reactiveMongoDatabaseFactory, converter) {

    private val secondaryReadPreference: ReadPreference =
        ReadPreference.secondaryPreferred(sessionStalenessSeconds.toLong(), TimeUnit.SECONDS)

    private val exceptionTranslator: PersistenceExceptionTranslator = mongoDatabaseFactory.exceptionTranslator

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

    override fun <T> createFlux(collectionName: String, callback: ReactiveCollectionCallback<T>): Flux<T> {
        val collectionPublisher = Mono.deferContextual { contextView: ContextView? ->
            doGetDatabase()
                .map { database: MongoDatabase? ->
                    getAndPrepareCollection(
                        shouldUseSecondary(
                            contextView!!
                        ), database!!, collectionName!!
                    )
                }
        }
        return collectionPublisher.flatMapMany { collection: MongoCollection<Document?>? ->
            callback.doInCollection(collection)
        }
            .onErrorMap(translateException())
    }

    override fun <T : Any?> createMono(collectionName: String, callback: ReactiveCollectionCallback<T>): Mono<T> {
        Assert.hasText(collectionName, "Collection name must not be null or empty!")
        Assert.notNull(callback, "ReactiveCollectionCallback must not be null!")
        val collectionPublisher = Mono.deferContextual { contextView: ContextView? ->
            doGetDatabase()
                .map { database: MongoDatabase? ->
                    getAndPrepareCollection(
                        shouldUseSecondary(
                            contextView!!
                        ), database!!, collectionName!!
                    )
                }
        }
        return collectionPublisher.flatMap<T?> { collection: MongoCollection<Document?>? ->
            Mono.from<T?>(
                callback.doInCollection(collection)
            )
        }
            .onErrorMap(translateException())
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

    private fun prepareCollection(
        secondary: Boolean,
        collection: MongoCollection<Document?>
    ): MongoCollection<Document?>? {
        return if (secondary) collection.withReadPreference(secondaryReadPreference) else collection
    }

    private fun shouldUseSecondary(contextView: ContextView): Boolean {
        return readReplicaEnabled() && contextView.hasKey(USE_REPLICA_CONTEXT_KEY)
    }

    private fun getAndPrepareCollection(
        secondary: Boolean,
        db: MongoDatabase,
        collectionName: String
    ): MongoCollection<Document?>? {
        return try {
            val collection = db.getCollection(
                collectionName,
                Document::class.java
            )
            prepareCollection(secondary, collection)
        } catch (e: RuntimeException) {
            throw potentiallyConvertRuntimeException(
                e,
                exceptionTranslator
            )
        }
    }

    private fun potentiallyConvertRuntimeException(
        ex: RuntimeException,
        exceptionTranslator: PersistenceExceptionTranslator
    ): RuntimeException {
        val resolved: RuntimeException? = exceptionTranslator.translateExceptionIfPossible(ex)
        return resolved ?: ex
    }

    private fun translateException(): Function<Throwable?, Throwable?> {
        return Function<Throwable?, Throwable?> { throwable: Throwable? ->
            if (throwable is RuntimeException) {
                potentiallyConvertRuntimeException(
                    throwable,
                    exceptionTranslator
                )
            } else {
                throwable
            }
        }
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

    companion object {
        val USE_REPLICA_CONTEXT_KEY = "USE_REPLICA_CONTEXT_KEY"
    }
}
