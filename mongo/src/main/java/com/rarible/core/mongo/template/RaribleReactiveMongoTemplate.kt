package com.rarible.core.mongo.template

import com.mongodb.reactivestreams.client.FindPublisher
import com.rarible.core.mongo.configuration.MongoProperties
import org.bson.Document
import org.springframework.data.mongodb.ReactiveMongoDatabaseFactory
import org.springframework.data.mongodb.core.FindPublisherPreparer
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.convert.MongoConverter
import reactor.core.publisher.Flux
import java.util.concurrent.TimeUnit

class RaribleReactiveMongoTemplate(
    reactiveMongoDatabaseFactory: ReactiveMongoDatabaseFactory,
    converter: MongoConverter,
    private val properties: MongoProperties,
) : ReactiveMongoTemplate(reactiveMongoDatabaseFactory, converter) {

    override fun <T : Any?> doFind(
        collectionName: String?,
        query: Document?,
        fields: Document?,
        entityClass: Class<T>?,
        preparer: FindPublisherPreparer?
    ): Flux<T> {
        return super.doFind(collectionName, query, fields, entityClass, MaxTimeAwarePreparer(preparer))
    }

    inner class MaxTimeAwarePreparer(private val delegate: FindPublisherPreparer?) : FindPublisherPreparer {
        override fun prepare(findPublisher: FindPublisher<Document>): FindPublisher<Document> {
            val publisher = delegate?.prepare(findPublisher) ?: findPublisher
            if (properties.maxTime != null) {
                return publisher.maxTime(properties.maxTime.toMillis(), TimeUnit.MILLISECONDS)
            }
            return publisher
        }
    }
}
