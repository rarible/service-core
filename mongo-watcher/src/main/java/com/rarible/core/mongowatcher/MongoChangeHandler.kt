package com.rarible.core.mongowatcher

import com.mongodb.MongoCommandException
import com.mongodb.client.model.changestream.ChangeStreamDocument
import com.mongodb.client.model.changestream.FullDocument
import com.rarible.core.task.RunTask
import com.rarible.core.task.TaskHandler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.reactive.asFlow
import org.bson.BsonDocument
import org.bson.BsonString
import org.bson.Document
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.data.mongodb.core.ReactiveMongoOperations
import reactor.core.publisher.Flux
import java.time.Instant

abstract class MongoChangeHandler(
    protected val collection: String,
    protected val mongo: ReactiveMongoOperations
) : TaskHandler<String> {

    protected abstract suspend fun onEvent(mongoEvent: MongoEvent)

    override val type: String
        get() = "MONGO_CHANGE_${collection}"

    override fun getAutorunParams(): List<RunTask> = listOf(RunTask(""))

    override fun runLongTask(from: String?, param: String): Flow<String> {
        return observe(from)
            .onErrorResume { error ->
                if (from != null && error is MongoCommandException
                    && (error.code == CHANGE_STREAM_FATAL_ERROR || error.code == CHANGE_STREAM_HISTORY_LOST)
                ) {
                    logger.error("Non resumable change stream error '${error.errorCodeName}', reset resume token")
                    observe(null)
                } else {
                    Flux.error(error)
                }
            }
            .asFlow()
            .onEach { onEvent(it) }
            .map { it.id }
    }

    private fun observe(from: String?): Flux<MongoEvent> =
        mongo.getCollection(collection)
            .flatMapMany {
                it.watch().apply {
                    fullDocument(FullDocument.UPDATE_LOOKUP)
                    from?.let { token -> resumeAfter(createResumeToken(token)) }
                }
            }
            .map { event ->
                val eventId = getEventId(event)
                val documentId = getDocumentId(event)
                val timestamp = getTimestamp(event)

                val database = getDatabase(event)
                val collection = getCollection(event)

                MongoEvent(
                    id = eventId,
                    operationType = event.operationType,
                    timestamp = timestamp,
                    documentId = documentId,
                    database = database,
                    collection = collection,
                    fullDocument = event.fullDocument,
                    updateDescription = event.updateDescription
                )
            }

    private fun getTimestamp(event: ChangeStreamDocument<Document>): Instant {
        return event.clusterTime?.time?.let { Instant.ofEpochSecond(it.toLong()) } ?: Instant.now()
    }

    private fun getDatabase(event: ChangeStreamDocument<Document>): String? {
        return event.namespaceDocument?.get("db")?.asString()?.value
    }

    private fun getCollection(event: ChangeStreamDocument<Document>): String? {
        return event.namespaceDocument?.get("coll")?.asString()?.value
    }

    private fun getDocumentId(event: ChangeStreamDocument<Document>): String? {
        val id = event.documentKey?.get("_id") ?: return null
        return when {
            id.isString -> {
                id.asString()?.value
            }
            id.isObjectId -> {
                id.asObjectId().value.toHexString()
            }
            else -> throw IllegalArgumentException("Unsupported type id in mongo type=${id.bsonType.name}")
        }
    }

    private fun getEventId(event: ChangeStreamDocument<Document>): String {
        val data = requireNotNull(event.resumeToken["_data"]) {
            "Event id must exist in change stream event $event"
        }
        return when {
            data.isString -> {
                data.asString().value
            }
            else -> throw IllegalArgumentException("Unsupported type id in mongo type=${data.bsonType.name}")
        }
    }

    private fun createResumeToken(nextEventId: String): BsonDocument {
        return BsonDocument("_data", BsonString(nextEventId))
    }

    private companion object {
        private val logger: Logger = LoggerFactory.getLogger(MongoChangeHandler::class.java)

        const val CHANGE_STREAM_FATAL_ERROR = 280
        const val CHANGE_STREAM_HISTORY_LOST = 286
    }
}