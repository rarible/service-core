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
import com.rarible.core.meta.resource.util.MetaLogger.logMetaLoading

class MetaResolver<K, M>(
    val name: String,
    private val metaUrlResolver: MetaUrlResolver<K>,
    private val metaUrlParser: MetaUrlParser<K>,
    private val rawMetaProvider: RawMetaProvider<K>,
    private val metaParser: MetaParser<K> = DefaultMetaParser(),
    private val metaMapper: MetaMapper<K, M>,
    private val urlCustomizers: List<MetaUrlCustomizer<K>>, // TODO do not forget to implement in ethereum!
    private val urlSanitizers: List<MetaUrlSanitizer<K>> = listOf(MetaUrlExtensionSanitizer())
) {

    private val metaMediaTypeResolver = MetaMediaTypeResolver()
    private val mediaTypes = listOf(
        "image/",
        "video/",
        "audio/",
        "model/",
        "html/"
    )

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

    private suspend fun resolveByCustomizedUrl(entityId: K, metaUrl: String): MetaResult<M>? {
        urlCustomizers.forEach { customizer ->
            val customizedUrl = customizer.customize(entityId, metaUrl)
            val meta = customizedUrl?.let { fetch(entityId, it) }
            if (meta != null) {
                logMetaLoading(entityId, "Custom meta URL worked for $entityId: $customizedUrl (original=$metaUrl)")
                return meta.copy(metaUrl = metaUrl)
            }
        }
        return null
    }

    private suspend fun resolveBySanitizedUrl(entityId: K, metaUrl: String): MetaResult<M>? {
        urlSanitizers.forEach { sanitizer ->
            val sanitizedUrl = sanitizer.sanitize(entityId, metaUrl)
            val meta = sanitizedUrl?.let { fetch(entityId, it) }
            if (meta != null) {
                logMetaLoading(entityId, "Sanitized meta URL worked for $entityId: $sanitizedUrl (original=$metaUrl)")
                return meta.copy(metaUrl = metaUrl)
            }
        }
        return null
    }

    private suspend fun fetch(entityId: K, metaUrl: String): MetaResult<M>? {
        metaUrl.ifNotBlank() ?: return null
        val resource = metaUrlParser.parse(entityId, metaUrl)
        val rawMeta = rawMetaProvider.getRawMeta(entityId, resource, metaParser)

        // JSON is valid, so we need just map it to the model
        if (rawMeta.parsed != null) {
            return try {
                mapMeta(entityId, metaUrl, rawMeta.parsed)
            } catch (e: Error) {
                logMetaLoading(
                    id = entityId,
                    message = "failed to map properties by URI $metaUrl: ${rawMeta.parsed}",
                    warn = true
                )
                throw e
            }
        }

        val resolved = metaMediaTypeResolver.resolveContent(metaUrl, rawMeta)
        val mimeType = resolved?.meta?.mimeType ?: return null
        val isMedia = mediaTypes.find { mimeType.startsWith(it) } != null
        return MetaResult(null, metaUrl, isMedia)
    }

    private fun mapMeta(
        entityId: K,
        metaUrl: String,
        json: ObjectNode
    ): MetaResult<M>? {
        val result = metaMapper.map(entityId, json)

        if (!metaMapper.isEmpty(result)) {
            return MetaResult(result, metaUrl, false)
        }
        logMetaLoading(entityId, "empty meta json received by URL: $metaUrl")
        return null
    }
}
