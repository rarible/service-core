package com.rarible.core.mongo.converter

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import org.bson.Document
import org.springframework.core.convert.converter.Converter

class DocumentToObjectNodeConverter(private val mapper: ObjectMapper) : Converter<Document, ObjectNode> {
    override fun convert(source: Document): ObjectNode? {
        return mapper.readTree(source.toJson()) as ObjectNode
    }
}

class ObjectNodeToDocumentConverter(private val mapper: ObjectMapper) : Converter<ObjectNode, Document> {
    override fun convert(source: ObjectNode): Document? {
        return Document.parse(mapper.writeValueAsString(source))
    }
}

class ObjectNodeToStringConverter(private val mapper: ObjectMapper) : Converter<ObjectNode, String> {
    override fun convert(source: ObjectNode): String? {
        return mapper.writeValueAsString(source)
    }
}

class StringToObjectNodeConverter(private val mapper: ObjectMapper) : Converter<String, ObjectNode> {
    override fun convert(source: String): ObjectNode? {
        return mapper.readTree(source) as ObjectNode
    }
}
