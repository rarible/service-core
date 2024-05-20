package com.rarible.core.meta.resolver

import com.fasterxml.jackson.databind.node.ObjectNode
import com.rarible.core.meta.resolver.parser.DefaultMetaParser
import com.rarible.core.meta.resolver.test.TestMeta
import com.rarible.core.meta.resolver.test.TestObjects
import com.rarible.core.meta.resolver.url.DefaultMetaUrlParser
import com.rarible.core.meta.resolver.url.MetaUrlCustomizer
import com.rarible.core.meta.resolver.url.MetaUrlExtensionSanitizer
import com.rarible.core.meta.resolver.url.MetaUrlResolver
import com.rarible.core.meta.resolver.url.MetaUrlSanitizer
import com.rarible.core.meta.resolver.util.getText
import com.rarible.core.test.data.randomString
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class MetaResolverTest {

    private val rawMetaProvider: RawMetaProvider<String> = mockk()
    private val metaUrlResolver: MetaUrlResolver<String> = mockk()
    private val urlParser = TestObjects.urlParser
    private val metaParser = DefaultMetaParser<String>()

    @Test
    fun `resolve - ok`() = runBlocking<Unit> {
        val resolver = createResolver()
        val entityId = randomString()
        val url = urlParser.parse("https://test.com/$entityId")!!
        val data = """{"name" : "Alice"}"""
        val rawMeta = RawMeta(metaParser.parse(entityId, data), data.toByteArray(), null)

        coEvery { metaUrlResolver.getUrl(entityId) } returns url.original
        coEvery { rawMetaProvider.getRawMeta(entityId, url, metaParser) } returns rawMeta

        val result = resolver.resolve(entityId)!!

        assertThat(result.metaUrl).isEqualTo(url.original)
        assertThat(result.meta?.name).isEqualTo("Alice")
        assertThat(result.isMedia).isFalse()
    }

    @Test
    fun `resolve - ok, media url`() = runBlocking<Unit> {
        val resolver = createResolver()
        val entityId = randomString()
        val url = urlParser.parse("https://test.com/$entityId")!!
        val data = "abc"
        val rawMeta = RawMeta(null, data.toByteArray(), "image/png")

        coEvery { metaUrlResolver.getUrl(entityId) } returns url.original
        coEvery { rawMetaProvider.getRawMeta(entityId, url, metaParser) } returns rawMeta

        val result = resolver.resolve(entityId)!!

        assertThat(result.metaUrl).isEqualTo(url.original)
        assertThat(result.meta).isNull()
        assertThat(result.isMedia).isTrue()
    }

    @Test
    fun `resolve - ok, by custom url`() = runBlocking<Unit> {
        val entityId = randomString()
        val url = "https://test.com/{id}"
        val customUrl = urlParser.parse("https://test.com/$entityId")!!
        val data = """{"name" : "Bob"}"""
        val rawMeta = RawMeta(metaParser.parse(entityId, data), data.toByteArray(), null)

        val resolver = createResolver(
            urlCustomizer = object : MetaUrlCustomizer<String> {
                override fun customize(entityId: String, metaUrl: String): String {
                    return metaUrl.replace("{id}", entityId)
                }
            }
        )

        coEvery { rawMetaProvider.getRawMeta(entityId, customUrl, metaParser) } returns rawMeta

        val result = resolver.resolve(entityId, url)!!

        assertThat(result.metaUrl).isEqualTo(url)
        assertThat(result.meta?.name).isEqualTo("Bob")
        assertThat(result.isMedia).isFalse()
    }

    @Test
    fun `resolve - ok, by sanitized url`() = runBlocking<Unit> {
        val entityId = randomString()
        val url = urlParser.parse("https://test.com/$entityId.json")!!
        val sanitizedUrl = urlParser.parse("https://test.com/$entityId")!!
        val data = """{"name" : "Nancy"}"""
        val rawMeta = RawMeta(metaParser.parse(entityId, data), data.toByteArray(), null)

        val resolver = createResolver(urlSanitizer = MetaUrlExtensionSanitizer())

        coEvery { rawMetaProvider.getRawMeta(entityId, url, metaParser) } returns RawMeta.EMPTY
        coEvery { rawMetaProvider.getRawMeta(entityId, sanitizedUrl, metaParser) } returns rawMeta

        val result = resolver.resolve(entityId, url.original)!!

        assertThat(result.metaUrl).isEqualTo(url.original)
        assertThat(result.meta?.name).isEqualTo("Nancy")
        assertThat(result.isMedia).isFalse()
    }

    @Test
    fun `resolve - ok, json instead of url`() = runBlocking<Unit> {
        val entityId = randomString()
        val url = """{"name" : "Cat"}"""

        val resolver = createResolver()

        val result = resolver.resolve(entityId, url)!!

        assertThat(result.metaUrl).isEqualTo(url)
        assertThat(result.meta?.name).isEqualTo("Cat")
    }

    @Test
    fun `resolve - fail, no url`() = runBlocking<Unit> {
        val resolver = createResolver()
        val entityId = randomString()

        coEvery { metaUrlResolver.getUrl(entityId) } returns ""

        val result = resolver.resolve(entityId)
        assertThat(result).isNull()
    }

    @Test
    fun `resolve - fail, corrupted json`() = runBlocking<Unit> {
        val resolver = createResolver()
        val entityId = randomString()
        val url = urlParser.parse("https://test.com/$entityId")!!
        val data = "not a json"
        val rawMeta = RawMeta(null, data.toByteArray(), null)

        coEvery { metaUrlResolver.getUrl(entityId) } returns url.original
        coEvery { rawMetaProvider.getRawMeta(entityId, url, metaParser) } returns rawMeta

        val result = resolver.resolve(entityId)
        assertThat(result).isNull()
    }

    @Test
    fun `resolve - fail, meta is empty`() = runBlocking<Unit> {
        val resolver = createResolver()
        val entityId = randomString()
        val url = urlParser.parse("https://test.com/$entityId")!!
        val data = "{}"
        val rawMeta = RawMeta(metaParser.parse(entityId, data), data.toByteArray(), null)

        coEvery { metaUrlResolver.getUrl(entityId) } returns url.original
        coEvery { rawMetaProvider.getRawMeta(entityId, url, metaParser) } returns rawMeta

        val result = resolver.resolve(entityId)

        assertThat(result).isNull()
    }

    @Test
    fun `resolve - fail, no json`() = runBlocking<Unit> {
        val resolver = createResolver()
        val entityId = randomString()
        val url = urlParser.parse("https://test.com/$entityId")!!

        coEvery { rawMetaProvider.getRawMeta(entityId, url, metaParser) } returns RawMeta.EMPTY

        val result = resolver.resolve(entityId, url.original)

        assertThat(result).isNull()
    }

    private fun createResolver(
        urlCustomizer: MetaUrlCustomizer<String>? = null,
        urlSanitizer: MetaUrlSanitizer<String>? = null,
    ): MetaResolver<String, TestMeta> {
        return MetaResolver(
            name = "test",
            metaUrlResolver = metaUrlResolver,
            metaUrlParser = DefaultMetaUrlParser(TestObjects.urlService),
            metaParser = metaParser,
            rawMetaProvider = rawMetaProvider,
            metaMapper = TestMetaMapper(),
            urlCustomizers = urlCustomizer?.let { listOf(it) } ?: emptyList(),
            urlSanitizers = urlSanitizer?.let { listOf(it) } ?: emptyList()
        )
    }

    class TestMetaMapper : MetaMapper<String, TestMeta> {
        override fun map(entityId: String, json: ObjectNode): TestMeta {
            return TestMeta(json.getText("name"))
        }

        override fun isEmpty(meta: TestMeta): Boolean {
            return meta.name == null
        }
    }
}
