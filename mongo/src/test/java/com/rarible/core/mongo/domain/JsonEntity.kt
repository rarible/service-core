package com.rarible.core.mongo.domain

import com.fasterxml.jackson.databind.node.ObjectNode
import org.bson.types.ObjectId
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document

@Document(collection = "json_entity")
data class JsonEntity(
    @Id
    val id: ObjectId,
    val value: ObjectNode
)
