package com.rarible.core.meta.resolver.parser

import com.fasterxml.jackson.core.json.JsonReadFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.rarible.core.meta.resolver.MetaResolverException
import com.rarible.core.meta.resource.util.MetaLogger.logMetaLoading
import org.apache.commons.codec.binary.Base64
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

object JsonMetaParser {

    private const val BASE_64_JSON_PREFIX = "data:application/json;base64,"
    private const val JSON_UTF8_PREFIX = "data:application/json;utf8,"
    private const val JSON_ASCII_PREFIX = "data:application/json;ascii,"
    private const val JSON_PREFIX = "data:application/json,"

    private val emptyChars = "\uFEFF".toCharArray()

    private val mapper = ObjectMapper().registerKotlinModule()
        .enable(JsonReadFeature.ALLOW_TRAILING_COMMA.mappedFeature())

    fun parse(id: String, data: String): ObjectNode {
        val trimmed = trim(data)
        return when {
            trimmed.startsWith(BASE_64_JSON_PREFIX) -> parseBase64(id, trimmed.removePrefix(BASE_64_JSON_PREFIX))
            trimmed.startsWith(JSON_UTF8_PREFIX) -> parseJson(id, trimmed.removePrefix(JSON_UTF8_PREFIX))
            trimmed.startsWith(JSON_ASCII_PREFIX) -> parseJson(id, trimmed.removePrefix(JSON_ASCII_PREFIX))
            trimmed.startsWith(JSON_PREFIX) -> parseJson(id, trimmed.removePrefix(JSON_PREFIX))
            isRawJson(trimmed) -> parseJson(id, trimmed)
            else -> throw MetaResolverException(
                "failed to parse Json: $data",
                status = MetaResolverException.Status.CORRUPTED_JSON
            )
        }
    }

    private fun parseBase64(itemId: String, data: String): ObjectNode {
        logMetaLoading(itemId, "parsing properties as Base64")
        val decodedJson = try {
            String(Base64.decodeBase64(data))
        } catch (e: Exception) {
            val errorMessage = "failed to decode Base64: ${e.message}"
            logMetaLoading(itemId, errorMessage, warn = true)

            throw MetaResolverException(errorMessage, status = MetaResolverException.Status.CORRUPTED_JSON)
        }
        return parseJson(itemId, decodedJson)
    }

    private fun parseJson(itemId: String, data: String): ObjectNode {
        return try {
            val decoded = if (isUrlEncodedRawJson(data)) {
                URLDecoder.decode(data, StandardCharsets.UTF_8.name())
            } else {
                data
            }
            mapper.readTree(decoded) as ObjectNode
        } catch (e: Exception) {
            val errorMessage = "failed to parse properties from json: ${e.message}"
            logMetaLoading(itemId, errorMessage, warn = true)

            throw MetaResolverException(errorMessage, status = MetaResolverException.Status.CORRUPTED_JSON)
        }
    }

    private fun isRawJson(data: String): Boolean {
        return (data.startsWith("{") && data.endsWith("}")) || isUrlEncodedRawJson(data)
    }

    private fun isUrlEncodedRawJson(data: String): Boolean {
        return (data.startsWith("%7b", true) && data.endsWith("%7d", true))
    }

    private fun trim(data: String): String {
        return data.trim { it.isWhitespace() || it in emptyChars }
    }
}
