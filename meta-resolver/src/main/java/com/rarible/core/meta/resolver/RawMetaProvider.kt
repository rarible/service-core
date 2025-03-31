package com.rarible.core.meta.resolver

import com.rarible.core.meta.resolver.cache.RawMetaCache
import com.rarible.core.meta.resolver.cache.RawMetaCacheService
import com.rarible.core.meta.resolver.parser.MetaParser
import com.rarible.core.meta.resolver.url.UrlService
import com.rarible.core.meta.resource.http.ExternalHttpClient
import com.rarible.core.meta.resource.model.HttpUrl
import com.rarible.core.meta.resource.model.UrlResource
import com.rarible.core.meta.resource.util.MetaLogger.logMetaLoading
import org.springframework.http.MediaType
import java.net.URL

class RawMetaProvider<K>(
    private val rawMetaCacheService: RawMetaCacheService,
    private val urlService: UrlService,
    private val externalHttpClient: ExternalHttpClient,
    private val proxyEnabled: Boolean,
    private val cacheEnabled: Boolean
) {

    suspend fun getRawMeta(
        blockchain: String,
        entityId: K,
        resource: UrlResource,
        parser: MetaParser<K>
    ): RawMeta {
        val cache = getCache(resource)
        getFromCache(cache, resource)?.let {
            return RawMeta(parser.parse(entityId, it), null, null)
        }

        // We want to use proxy only for regular HTTP urls, IPFS URLs should not be proxied
        // because we use our own IPFS nodes
        val useProxy = (resource is HttpUrl) && proxyEnabled

        val fetched = fetch(blockchain = blockchain, resource = resource, entityId = entityId, useProxy = useProxy)
        val bytes = fetched.second ?: return RawMeta.EMPTY
        val mimeType = fetched.first?.toString()

        val fetchedJsonString = String(bytes)

        if (bytes.size > 1_000_000) {
            logMetaLoading(
                entityId, "suspiciously big meta Json ${bytes.size} for ${resource.original}", warn = true
            )
        }

        val parsed = try {
            parser.parse(entityId, fetchedJsonString)
        } catch (e: Exception) {
            logMetaLoading(
                id = entityId,
                message = e.message ?: "Failed to parse meta JSON for $entityId: ",
                warn = true
            )
            null
        }

        // We are going to save only successfully parsed jsons
        if (parsed != null) {
            updateCache(cache, resource, fetchedJsonString)
        }

        return RawMeta(parsed, bytes, mimeType)
    }

    private fun getCache(resource: UrlResource): RawMetaCache? {
        return if (cacheEnabled) {
            rawMetaCacheService.getCache(resource)
        } else {
            null
        }
    }

    private suspend fun getFromCache(cache: RawMetaCache?, resource: UrlResource): String? {
        cache ?: return null
        val fromCache = cache.get(resource) ?: return null
        return fromCache.content
    }

    private suspend fun updateCache(cache: RawMetaCache?, resource: UrlResource, result: String?) {
        if (result.isNullOrBlank() || cache == null) {
            return
        }
        cache.save(resource, result)
    }

    private suspend fun fetch(
        resource: UrlResource,
        blockchain: String,
        entityId: K,
        useProxy: Boolean = false
    ): Pair<MediaType?, ByteArray?> {
        val internalUrl = urlService.resolveInternalHttpUrl(resource)

        if (internalUrl == resource.original) {
            logMetaLoading(entityId, "Fetching meta by URL $internalUrl")
        } else {
            logMetaLoading(
                entityId, "Fetching meta by URL $internalUrl (original URL is ${resource.original})"
            )
        }

        try {
            URL(internalUrl)
        } catch (e: Throwable) {
            logMetaLoading(entityId, "Corrupted URL: $internalUrl, $e")
            return null to null
        }

        // There are several points:
        // 1. Without proxy some sites block our requests (403/429)
        // 2. With proxy some sites block us too, but respond fine without proxy
        // 3. It is better to avoid proxy requests since they are paid
        // So even if useProxy specified we want to try fetch data without it first
        val withoutProxy = doFetch(blockchain = blockchain, url = internalUrl, entityId = entityId, useProxy = false)

        // Second try with proxy, if needed
        return if (withoutProxy.second == null && useProxy) {
            doFetch(blockchain = blockchain, url = internalUrl, entityId = entityId, useProxy = true)
        } else {
            withoutProxy
        }
    }

    private suspend fun doFetch(
        blockchain: String,
        url: String,
        entityId: K,
        useProxy: Boolean = false
    ): Pair<MediaType?, ByteArray?> {
        return externalHttpClient.getBodyBytes(
            blockchain = blockchain,
            url = url,
            id = entityId.toString(),
            useProxy = useProxy
        )
    }
}
