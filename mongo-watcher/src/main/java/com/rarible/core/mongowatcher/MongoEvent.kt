package com.rarible.core.mongowatcher

import com.mongodb.client.model.changestream.OperationType
import com.mongodb.client.model.changestream.UpdateDescription
import org.bson.Document
import java.time.Instant

data class MongoEvent(
    val id: String,
    val operationType: OperationType,
    val timestamp: Instant,
    val database: String?,
    val collection: String?,
    val documentId: String?,
    val fullDocument: Document?,
    val updateDescription: UpdateDescription?
) {
    override fun toString(): String {
        return "MongoEvent(id=${id}, documentId=${documentId})"
    }
}