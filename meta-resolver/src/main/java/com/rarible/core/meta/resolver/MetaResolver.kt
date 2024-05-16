package com.rarible.core.meta.resolver

import com.fasterxml.jackson.databind.node.ObjectNode
import com.rarible.core.common.ifNotBlank
import com.rarible.core.meta.resolver.parser.DefaultMetaParser
import com.rarible.core.meta.resolver.parser.JsonMetaParser
import com.rarible.core.meta.resolver.parser.MetaParser
import com.rarible.core.meta.resolver.url.MetaUrlCustomizer
import com.rarible.core.meta.resolver.url.MetaUrlExtensionSanitizer
import com.rarible.core.meta.resolver.url.MetaUrlParser
import com.rarible.core.meta.resolver.url.MetaUrlResolver
import com.rarible.core.meta.resolver.url.MetaUrlSanitizer
import com.rarible.core.meta.resolver.util.logMetaLoading

class MetaResolver<K, M : Meta>(
    val name: String,
    private val metaUrlResolver: MetaUrlResolver<K>,
    private val metaUrlParser: MetaUrlParser<K>,
    private val rawMetaProvider: RawMetaProvider<K>,
    private val metaParser: MetaParser<K> = DefaultMetaParser(),
    private val metaMapper: MetaMapper<K, M>,
    private val urlCustomizers: List<MetaUrlCustomizer<K>>, // TODO do not forget to implement in ethereum!
    private val urlSanitizers: List<MetaUrlSanitizer<K>> = listOf(MetaUrlExtensionSanitizer())
) {

    suspend fun resolve(entityId: K): MetaResult<M>? {
        val metaUrl = metaUrlResolver.getUrl(entityId)?.trim()
        if (metaUrl.isNullOrBlank()) {
            logMetaLoading(entityId, "empty metadata URL", warn = true)
            return null
        }
        logMetaLoading(entityId, "got meta URL: $metaUrl")
        return resolve(entityId, metaUrl)
    }

    suspend fun resolve(entityId: K, metaUrl: String): MetaResult<M>? {
        val meta = resolveInner(entityId, metaUrl)
        return meta?.let { MetaResult(meta, metaUrl) }
    }

    private suspend fun resolveInner(entityId: K, metaUrl: String): M? {
        // Some of the URLs might be customized like with {id} substitution - so check such cases first
        val byCustomUrl = resolveByCustomizedUrl(entityId, metaUrl)
        if (byCustomUrl != null) {
            return byCustomUrl
        }

        // Sometimes there could be a json instead of URL
        val json = runCatching { JsonMetaParser.parse(entityId.toString(), metaUrl) }.getOrNull()
        if (json != null) {
            return mapMeta(entityId, metaUrl, json)
        }

        // If meta fetched by unmodified URL - ok, return it
        val fetched = fetch(entityId, metaUrl)
        if (fetched != null) {
            return fetched
        }

        // TODO in ethereum there is also post-processing for empty names
        // The last hope is to try to sanitize URL
        return resolveBySanitizedUrl(entityId, metaUrl)
    }

    private suspend fun resolveByCustomizedUrl(entityId: K, metaUrl: String): M? {
        urlCustomizers.forEach { customizer ->
            val customizedUrl = customizer.customize(entityId, metaUrl)
            val meta = customizedUrl?.let { fetch(entityId, it) }
            if (meta != null) {
                logMetaLoading(entityId, "Custom meta URL worked for $entityId: $customizedUrl (original=$metaUrl)")
                return meta
            }
        }
        return null
    }

    private suspend fun resolveBySanitizedUrl(entityId: K, metaUrl: String): M? {
        urlSanitizers.forEach { sanitizer ->
            val sanitizedUrl = sanitizer.sanitize(entityId, metaUrl)
            val meta = sanitizedUrl?.let { fetch(entityId, it) }
            if (meta != null) {
                logMetaLoading(entityId, "Sanitized meta URL worked for $entityId: $sanitizedUrl (original=$metaUrl)")
                return meta
            }
        }
        return null
    }

    private suspend fun fetch(entityId: K, metaUrl: String): M? {
        metaUrl.ifNotBlank() ?: return null
        val resource = metaUrlParser.parse(entityId, metaUrl)
        val json = rawMetaProvider.getMetaJson(entityId, resource) ?: return null

        if (json.length > 1_000_000) {
            logMetaLoading(
                entityId, "suspiciously big item properties ${json.length} for $metaUrl", warn = true
            )
        }

        val parsed = metaParser.parse(entityId, json)

        return try {
            mapMeta(entityId, metaUrl, parsed)
        } catch (e: Error) {
            logMetaLoading(entityId, "failed to parse properties by URI: $metaUrl", warn = true)
            throw e
        }
    }

    private fun mapMeta(
        entityId: K,
        metaUrl: String,
        json: ObjectNode
    ): M? {
        val result = metaMapper.map(entityId, json)

        if (!result.isEmpty()) {
            return result
        }
        logMetaLoading(entityId, "empty meta json received by URL: $metaUrl")
        return null
    }
}
