package com.rarible.core.meta.resolver

import com.rarible.core.meta.resolver.cache.RawMetaCacheService
import com.rarible.core.meta.resolver.test.TestMeta
import com.rarible.core.meta.resolver.test.TestObjects
import com.rarible.core.meta.resolver.url.DefaultMetaUrlParser
import com.rarible.core.meta.resource.http.DefaultHttpClient
import com.rarible.core.meta.resource.http.ExternalHttpClient
import com.rarible.core.meta.resource.http.ProxyHttpClient
import com.rarible.core.meta.resource.http.builder.DefaultWebClientBuilder
import com.rarible.core.meta.resource.http.builder.ProxyWebClientBuilder
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

// For manual run only
@Disabled
class MetaResolverMt {

    private val defaultWebClientBuilder = DefaultWebClientBuilder(followRedirect = true)

    private val proxyWebClientBuilder = ProxyWebClientBuilder(
        readTimeout = 10000,
        connectTimeout = 3000,
        proxyUrl = "",
        followRedirect = true
    )

    private val defaultHttpClient = DefaultHttpClient(
        builder = defaultWebClientBuilder,
        requestTimeout = 20000
    )
    private val proxyHttpClient = ProxyHttpClient(
        builder = proxyWebClientBuilder,
        requestTimeout = 20000
    )

    private val externalHttpClient = ExternalHttpClient(
        defaultClient = defaultHttpClient,
        proxyClient = proxyHttpClient,
        customClients = emptyList()
    )

    private val rawMetaProvider: RawMetaProvider<String> = RawMetaProvider(
        rawMetaCacheService = RawMetaCacheService(emptyList()),
        urlService = TestObjects.urlService,
        externalHttpClient = externalHttpClient,
        proxyEnabled = false,
        cacheEnabled = false
    )

    private val resolver = MetaResolver(
        name = "test",
        metaUrlResolver = mockk(),
        metaUrlParser = DefaultMetaUrlParser(TestObjects.urlService),
        rawMetaProvider = rawMetaProvider,
        metaMapper = MetaResolverTest.TestMetaMapper(),
        urlCustomizers = emptyList(),
        urlSanitizers = emptyList()
    )

    @Test
    fun `resolve json`() = runBlocking<Unit> {
        val url = "https://polygon-piggybox-meta-assets-bucket.s3.us-west-2.amazonaws.com/piggybox/meta/824468.json"
        val result = resolver.resolve("1", url)!!

        assertThat(result.metaUrl).isEqualTo(url)
        assertThat(result.isMedia).isFalse()
        assertThat(result.meta).isEqualTo(TestMeta("Piggybox #824468"))
    }

    @Test
    fun `resolve media`() = runBlocking<Unit> {
        val url = "https://polygon-piggybox-meta-assets-bucket.s3.us-west-2.amazonaws.com/piggybox/image/824468.png"
        val result = resolver.resolve("1", url)!!

        assertThat(result.metaUrl).isEqualTo(url)
        assertThat(result.isMedia).isTrue()
        assertThat(result.meta).isNull()
    }
}
