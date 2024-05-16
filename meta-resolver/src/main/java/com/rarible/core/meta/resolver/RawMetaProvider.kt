package com.rarible.core.meta.resolver

import com.rarible.core.meta.resolver.cache.RawMetaCache
import com.rarible.core.meta.resolver.cache.RawMetaCacheService
import com.rarible.core.meta.resolver.url.UrlService
import com.rarible.core.meta.resolver.util.logMetaLoading
import com.rarible.core.meta.resource.http.ExternalHttpClient
import com.rarible.core.meta.resource.model.HttpUrl
import com.rarible.core.meta.resource.model.UrlResource
import java.net.URL

class RawMetaProvider<K>(
    private val rawMetaCacheService: RawMetaCacheService,
    private val urlService: UrlService,
    private val externalHttpClient: ExternalHttpClient,
    private val proxyEnabled: Boolean,
    private val cacheEnabled: Boolean
) {

    suspend fun getMetaJson(entityId: K, resource: UrlResource): String? {
        val cache = getCache(resource)
        getFromCache(cache, resource)?.let { return it }

        // We want to use proxy only for regular HTTP urls, IPFS URLs should not be proxied
        // because we want to use our own IPFS nodes in the nearest future
        val useProxy = (resource is HttpUrl) && proxyEnabled

        val fetched = fetch(resource, entityId, useProxy)
        updateCache(cache, resource, fetched)

        return fetched
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

    private suspend fun fetch(resource: UrlResource, entityId: K, useProxy: Boolean = false): String? {
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
            return null
        }

        // There are several points:
        // 1. Without proxy some sites block our requests (403/429)
        // 2. With proxy some sites block us too, but respond fine without proxy
        // 3. It is better to avoid proxy requests since they are paid
        // So even if useProxy specified we want to try fetch data without it first
        val withoutProxy = safeFetch(internalUrl, entityId, false)

        // Second try with proxy, if needed
        return if (withoutProxy == null && useProxy) {
            safeFetch(internalUrl, entityId, true)
        } else {
            withoutProxy
        }
    }

    private suspend fun safeFetch(url: String, entityId: K, useProxy: Boolean = false): String? {
        return try {
            externalHttpClient.getBody(
                url = url,
                id = entityId.toString(),
                useProxy = useProxy
            )
        } catch (e: Exception) {
            logMetaLoading(entityId, "Failed to receive property string via URL (proxy: $useProxy) $url $e")
            null
        }
    }
}
