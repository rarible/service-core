package com.rarible.core.meta.resolver

import com.rarible.core.common.nowMillis
import com.rarible.core.meta.resolver.cache.RawMetaCache
import com.rarible.core.meta.resolver.cache.RawMetaCacheService
import com.rarible.core.meta.resolver.cache.RawMetaEntry
import com.rarible.core.meta.resolver.parser.DefaultMetaParser
import com.rarible.core.meta.resolver.test.TestObjects
import com.rarible.core.meta.resource.http.ExternalHttpClient
import com.rarible.core.test.data.randomString
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType

class RawMetaProviderTest {

    private lateinit var rawPropertiesProvider: RawMetaProvider<String>

    private val httpClient: ExternalHttpClient = mockk()
    private val cache: RawMetaCache = mockk()

    private val urlParser = TestObjects.urlParser

    private val parser = DefaultMetaParser<String>()
    private val cid = "QmeqeBpsYTuJL8AZhY9fGBeTj9QuvMVqaZeRWFnjA24QEE"
    private val entityId = randomString()

    @BeforeEach
    fun beforeEach() {
        rawPropertiesProvider = createProvider(enableCache = true)
        clearMocks(cache, httpClient)
        coEvery { cache.get(any()) } returns null
    }

    @Test
    fun `cacheable url - cached`() = runBlocking<Unit> {
        val path = "$cid/${randomString()}"
        val urlResource = urlParser.parse("https://ipfs.io/ipfs/$path")!!
        val json = """{"name" : "${randomString()}"}"""

        coEvery {
            httpClient.getBodyBytes(blockchain = "ethereum", url = any(), id = entityId, useProxy = false)
        } returns (MediaType.APPLICATION_JSON to json.toByteArray())

        coEvery { cache.isSupported(urlResource) } returns true
        coEvery { cache.save(urlResource, json) } returns mockk()

        val result = rawPropertiesProvider.getRawMeta("ethereum", entityId, urlResource, parser)

        // Content returned and cached
        coVerify(exactly = 1) { cache.save(urlResource, json) }
        assertThat(result.parsed).isEqualTo(parser.parse(entityId, json))
    }

    @Test
    fun `cacheable url - proxy not used`() = runBlocking<Unit> {
        rawPropertiesProvider = createProvider(enableCache = true, enableProxy = true)
        val path = "$cid/${randomString()}"
        val urlResource = urlParser.parse("https://ipfs.io/ipfs/$path")!!
        val json = """{"name" : "${randomString()}"}"""

        coEvery {
            httpClient.getBodyBytes(blockchain = "ethereum", url = any(), id = entityId, useProxy = false)
        } returns (MediaType.APPLICATION_JSON to json.toByteArray())

        coEvery { cache.isSupported(urlResource) } returns true
        coEvery { cache.save(urlResource, json) } returns mockk()

        val result = rawPropertiesProvider.getRawMeta(blockchain = "ethereum", entityId, urlResource, parser)

        // Content returned and cached
        assertThat(result.parsed).isEqualTo(parser.parse(entityId, json))
        // Proxy not used since we fetched data from IPFS
        coVerify(exactly = 1) {
            httpClient.getBodyBytes(
                blockchain = "ethereum",
                url = any(),
                id = entityId,
                useProxy = false
            )
        }
    }

    @Test
    fun `cacheable url - cache disabled`() = runBlocking<Unit> {
        rawPropertiesProvider = createProvider(enableCache = false)
        val path = "$cid/${randomString()}"
        val urlResource = urlParser.parse("https://ipfs.io/ipfs/$path")!!
        val json = """{"name" : "${randomString()}"}"""

        coEvery {
            httpClient.getBodyBytes(blockchain = "ethereum", url = any(), id = entityId)
        } returns (MediaType.APPLICATION_JSON to json.toByteArray())

        val result = rawPropertiesProvider.getRawMeta(blockchain = "ethereum", entityId, urlResource, parser)

        // Should not be cached since cache is disabled
        coVerify(exactly = 0) { cache.save(any(), any()) }
        assertThat(result.parsed).isEqualTo(parser.parse(entityId, json))
    }

    @Test
    fun `cacheable url - not cached, content is not a json`() = runBlocking<Unit> {
        val path = "$cid/${randomString()}"
        val urlResource = urlParser.parse("ipfs://$path")!!
        val json = "not a json"

        coEvery {
            httpClient.getBodyBytes(blockchain = "ethereum", url = any(), id = entityId)
        } returns (MediaType.APPLICATION_JSON to json.toByteArray())

        coEvery { cache.isSupported(urlResource) } returns true

        val result = rawPropertiesProvider.getRawMeta(blockchain = "ethereum", entityId, urlResource, parser)

        coVerify(exactly = 0) { cache.save(any(), any()) }
        assertThat(result.bytes).isEqualTo(json.toByteArray())
    }

    @Test
    fun `cacheable url - not cached, content is null`() = runBlocking<Unit> {
        val path = "$cid/${randomString()}"
        val urlResource = urlParser.parse("ipfs://$path")!!

        // Content not resolved
        coEvery { httpClient.getBodyBytes(blockchain = "ethereum", url = any(), id = entityId) } returns (null to null)
        coEvery { cache.isSupported(urlResource) } returns true

        rawPropertiesProvider.getRawMeta(blockchain = "ethereum", entityId, urlResource, parser)

        coVerify(exactly = 0) { cache.save(any(), any()) }
    }

    @Test
    fun `cacheable url - from cache`() = runBlocking<Unit> {
        val path = "$cid/${randomString()}"
        val urlResource = urlParser.parse(path)!!
        val json = """{"name" : "${randomString()}"}"""

        val entry = RawMetaEntry(
            url = "ipfs://$path",
            updatedAt = nowMillis(),
            content = json
        )

        coEvery { cache.isSupported(urlResource) } returns true
        coEvery { cache.get(urlResource) } returns entry

        val result = rawPropertiesProvider.getRawMeta(blockchain = "ethereum", entityId, urlResource, parser)

        // Content returned and cached
        assertThat(result.parsed).isEqualTo(parser.parse(entityId, json))
    }

    @Test
    fun `not cacheable url`() = runBlocking<Unit> {
        val urlResource = urlParser.parse("https://localhost:8080/abc")!!
        val json = """{"name" : "${randomString()}"}"""

        coEvery {
            httpClient.getBodyBytes(blockchain = "ethereum", url = any(), id = entityId, useProxy = any())
        } returns (MediaType.APPLICATION_JSON to json.toByteArray())
        coEvery { cache.isSupported(urlResource) } returns false

        rawPropertiesProvider.getRawMeta(blockchain = "ethereum", entityId, urlResource, parser)

        // Not cached
        coVerify(exactly = 0) { cache.save(any(), any()) }
    }

    @Test
    fun `not cacheable url - proxy used`() = runBlocking<Unit> {
        rawPropertiesProvider = createProvider(enableCache = true, enableProxy = true)
        val json = """{"name" : "${randomString()}"}"""

        val urlResource = urlParser.parse("https://test.com/${randomString()}")!!

        // First call should be executed without proxy
        coEvery {
            httpClient.getBodyBytes(blockchain = "ethereum", url = any(), id = entityId, useProxy = false)
        } returns (null to null)

        // Since direct request has failed, proxy request should be executed
        coEvery {
            httpClient.getBodyBytes(blockchain = "ethereum", url = any(), id = entityId, useProxy = true)
        } returns (MediaType.APPLICATION_JSON to json.toByteArray())

        coEvery { cache.isSupported(urlResource) } returns true
        coEvery { cache.save(urlResource, json) } returns mockk()

        rawPropertiesProvider.getRawMeta(blockchain = "ethereum", entityId, urlResource, parser)

        // Content is not cached since it is not an IPFS URL
        coVerify(exactly = 1) {
            httpClient.getBodyBytes(
                blockchain = "ethereum",
                url = any(),
                id = entityId,
                useProxy = false
            )
        }
        coVerify(exactly = 1) {
            httpClient.getBodyBytes(
                blockchain = "ethereum",
                url = any(),
                id = entityId,
                useProxy = true
            )
        }
    }

    @Test
    fun `not cacheable url - proxy not used`() = runBlocking<Unit> {
        rawPropertiesProvider = createProvider(enableCache = true, enableProxy = true)
        val json = """{"name" : "${randomString()}"}"""

        val urlResource = urlParser.parse("https://test.com/${randomString()}")!!

        // First call should be executed without proxy - and it returns data
        coEvery {
            httpClient.getBodyBytes(blockchain = "ethereum", url = any(), id = entityId, useProxy = false)
        } returns (MediaType.APPLICATION_JSON to json.toByteArray())

        coEvery { cache.isSupported(urlResource) } returns true
        coEvery { cache.save(urlResource, json) } returns mockk()

        rawPropertiesProvider.getRawMeta(blockchain = "ethereum", entityId, urlResource, parser)

        // Even if useProxy == true, proxy should not be used since we got data via direct request
        coVerify(exactly = 1) {
            httpClient.getBodyBytes(
                blockchain = "ethereum",
                url = any(),
                id = entityId,
                useProxy = false
            )
        }
        coVerify(exactly = 0) {
            httpClient.getBodyBytes(
                blockchain = "ethereum",
                url = any(),
                id = entityId,
                useProxy = true
            )
        }
    }

    private fun createProvider(enableCache: Boolean = false, enableProxy: Boolean = false): RawMetaProvider<String> {
        return RawMetaProvider(
            rawMetaCacheService = RawMetaCacheService(listOf(cache)),
            urlService = TestObjects.urlService,
            externalHttpClient = httpClient,
            proxyEnabled = enableProxy,
            cacheEnabled = enableCache
        )
    }
}
