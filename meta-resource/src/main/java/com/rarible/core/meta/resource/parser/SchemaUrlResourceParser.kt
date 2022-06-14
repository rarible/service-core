package com.rarible.core.meta.resource.parser

import com.rarible.core.meta.resource.model.SchemaMapping
import com.rarible.core.meta.resource.model.SchemaUrl

class SchemaUrlResourceParser(
    schemaMappings: List<SchemaMapping> = DEFAULT_SCHEMA_MAPPING
) : UrlResourceParser<SchemaUrl> {

    private val bySchema = schemaMappings.associateBy { it.schema }

    override fun parse(url: String): SchemaUrl? {
        val schemaEndIndex = url.indexOf(SCHEMA_DELIMITER)
        if (schemaEndIndex < 0) {
            return null
        }
        val schema = url.substring(0, schemaEndIndex)
        val gateway = bySchema[schema]?.gateway ?: return null
        val path = url.substring(schemaEndIndex + SCHEMA_DELIMITER.length)

        return SchemaUrl(
            original = url,
            gateway = gateway,
            schema = schema,
            path = path,
        )
    }

    companion object {

        const val SCHEMA_DELIMITER = "://"

        val DEFAULT_SCHEMA_MAPPING = listOf(SchemaMapping("ar", "https://arweave.net"))
    }
}
