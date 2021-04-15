package com.rarible.core.mongo.domain

import org.bson.types.ObjectId
import org.springframework.data.mongodb.core.mapping.Document

@Document
data class BsonTest(
    val id: ObjectId = ObjectId.get(),
    val value: String?
)