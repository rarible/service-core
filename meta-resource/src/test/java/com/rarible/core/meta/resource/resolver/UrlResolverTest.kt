package com.rarible.core.meta.resource.resolver

import com.rarible.core.meta.resource.ArweaveUrl
import com.rarible.core.meta.resource.ArweaveUrl.Companion.ARWEAVE_GATEWAY
import com.rarible.core.meta.resource.ConstantGatewayProvider
import com.rarible.core.meta.resource.LegacyIpfsGatewaySubstitutor
import com.rarible.core.meta.resource.RandomGatewayProvider
import com.rarible.core.meta.resource.ResourceTestData.CID
import com.rarible.core.meta.resource.ResourceTestData.IPFS_CUSTOM_GATEWAY
import com.rarible.core.meta.resource.ResourceTestData.IPFS_PUBLIC_GATEWAY
import com.rarible.core.meta.resource.UrlResource
import com.rarible.core.meta.resource.cid.CidV1Validator
import com.rarible.core.meta.resource.parser.ArweaveUrlResourceParser
import com.rarible.core.meta.resource.parser.CidUrlResourceParser
import com.rarible.core.meta.resource.parser.DefaultUrlResourceParserProvider
import com.rarible.core.meta.resource.parser.HttpUrlResourceParser
import com.rarible.core.meta.resource.parser.UrlResourceProcessor
import com.rarible.core.meta.resource.parser.ipfs.AbstractIpfsUrlResourceParser
import com.rarible.core.meta.resource.parser.ipfs.ForeignIpfsUrlResourceParser
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class UrlResolverTest {

    private val cidOneValidator = CidV1Validator()
    private val foreignIpfsUrlResourceParser = ForeignIpfsUrlResourceParser(
        cidOneValidator = cidOneValidator
    )

    private val ipfsGatewayResolver = IpfsGatewayResolver(
        publicGatewayProvider = ConstantGatewayProvider(IPFS_PUBLIC_GATEWAY),
        innerGatewaysProvider = RandomGatewayProvider(listOf(IPFS_PUBLIC_GATEWAY)),
        customGatewaysResolver = LegacyIpfsGatewaySubstitutor(listOf(IPFS_CUSTOM_GATEWAY))
    )

    private val defaultUrlResourceParserProvider = DefaultUrlResourceParserProvider(
        arweaveUrlParser = ArweaveUrlResourceParser(),
        foreignIpfsUrlResourceParser = foreignIpfsUrlResourceParser,
        abstractIpfsUrlResourceParser = AbstractIpfsUrlResourceParser(),
        cidUrlResourceParser = CidUrlResourceParser(cidOneValidator),
        httpUrlParser = HttpUrlResourceParser()
    )

    private val urlResourceProcessor = UrlResourceProcessor(
        urlResourceParserProvider = defaultUrlResourceParserProvider
    )

    private val urlResolver = UrlResolver(
        ipfsGatewayResolver = ipfsGatewayResolver,
        ipfsCidGatewayResolver = IpfsCidGatewayResolver(
            publicGatewayProvider = ConstantGatewayProvider(IPFS_PUBLIC_GATEWAY),
            innerGatewaysProvider = RandomGatewayProvider(listOf(IPFS_PUBLIC_GATEWAY))
        ),
        arweaveGatewayResolver = ArweaveGatewayResolver(
            arweaveGatewayProvider = ConstantGatewayProvider(ARWEAVE_GATEWAY)
        ),
        simpleHttpGatewayResolver = SimpleHttpGatewayResolver()
    )

    // TODO Test for Arveawe

    @Test
    fun `foreign ipfs urls - replaced by public gateway`() {
        // Broken IPFS URL
        assertFixedIpfsUrl("htt://mypinata.com/ipfs/$CID", CID)

        // Abstract IPFS urls with /ipfs/ path and broken slashes
        assertFixedIpfsUrl("ipfs:/ipfs/$CID", CID)
        assertFixedIpfsUrl("ipfs://ipfs/$CID", CID)
        assertFixedIpfsUrl("ipfs:///ipfs/$CID", CID)
        assertFixedIpfsUrl("ipfs:////ipfs/$CID", CID)

        assertFixedIpfsUrl("ipfs:////ipfs/$CID", CID)
        assertFixedIpfsUrl("ipfs:////ipfs//$CID", CID)
        assertFixedIpfsUrl("ipfs:////ipfs///$CID", CID)
    }

    @Test
    fun `foreign ipfs urls - original gateway kept`() {
        // Regular IPFS URL
        assertOriginalIpfsUrl("https://ipfs.io/ipfs/$CID")
        // Regular IPFS URL with 2 /ipfs/ parts
        assertOriginalIpfsUrl("https://ipfs.io/ipfs/something/ipfs/$CID")
        // Regular IPFS URL but without CID
        assertOriginalIpfsUrl("http://ipfs.io/ipfs/123.jpg")
    }

    @Test
    fun `prefixed ipfs urls`() {
//        assertFixedIpfsUrl("ipfs:/folder/$CID/abc .json", "folder/$CID/abc%20.json")  //SPACE
        assertFixedIpfsUrl("ipfs://folder/abc", "folder/abc")
        assertFixedIpfsUrl("ipfs:///folder/subfolder/$CID", "folder/subfolder/$CID")
        assertFixedIpfsUrl("ipfs:////$CID", CID)

        // Various case of ipfs prefix
        assertFixedIpfsUrl("IPFS://$CID", CID)
        assertFixedIpfsUrl("Ipfs:///$CID", CID)

        // Abstract IPFS urls with /ipfs/ path and broken slashes without a CID
        assertFixedIpfsUrl("ipfs:/ipfs/abc", "abc")
        assertFixedIpfsUrl("ipfs://ipfs/folder/abc", "folder/abc")
        assertFixedIpfsUrl("ipfs:///ipfs/abc", "abc")
    }

    @Test
    fun `foreign ipfs urls - replaced by internal gateway`() {
        val result = resolveInnerHttpUrl("https://dweb.link/ipfs/$CID/1.png")
        assertThat(result)
            .isEqualTo("$IPFS_PUBLIC_GATEWAY/ipfs/$CID/1.png")
    }

    @Test
    fun `single sid`() {
        assertFixedIpfsUrl(CID, CID)
        assertFixedIpfsUrl("$CID/532.json", "$CID/532.json")
    }

    @Test
    fun `regular url`() {
        val https = "https://api.t-o-s.xyz/ipfs/gucci/8.gif"
        val http = "http://api.guccinfts.xyz/ipfs/8"

        assertThat(resolvePublicHttpUrl(http)).isEqualTo(http)
        assertThat(resolvePublicHttpUrl(https)).isEqualTo(https)
    }

    @Test
    fun `replace legacy`() {
        assertThat(resolveInnerHttpUrl("$IPFS_CUSTOM_GATEWAY/ipfs/$CID"))
            .isEqualTo("$IPFS_PUBLIC_GATEWAY/ipfs/$CID")
    }

    @Test
    fun `Thrown Unsupported Exception`() {
        val thrown = assertThrows(
            UnsupportedOperationException::class.java,
            { urlResolver.resolveInnerLink(UnsupportedResource("test")) },
            "UnsupportedOperationException error was expected"
        )

        assertEquals("Unsupported resolving for com.rarible.core.meta.resource.resolver.UnsupportedResource", thrown.message)
    }

    @Test
    fun `Arweave full link`() {
        assertThat(
            urlResolver.resolvePublicLink(
                ArweaveUrl(
                    original = "ar://lVS0SkeSF8_alma1ayYMZcH9VSMLrmhAmikrDyshUcg",
                    originalGateway = null,
                    path = "/lVS0SkeSF8_alma1ayYMZcH9VSMLrmhAmikrDyshUcg"
                )
            )
        ).isEqualTo("https://arweave.net/lVS0SkeSF8_alma1ayYMZcH9VSMLrmhAmikrDyshUcg")
    }

    @Test
    fun `Arweave shortlink link`() {
        assertThat(
            urlResolver.resolvePublicLink(
                ArweaveUrl(
                    original = "https://arweave.net/lVS0SkeSF8_alma1ayYMZcH9VSMLrmhAmikrDyshUcg",
                    originalGateway = "https://arweave.net",
                    path = "/lVS0SkeSF8_alma1ayYMZcH9VSMLrmhAmikrDyshUcg"
                )
            )
        ).isEqualTo("https://arweave.net/lVS0SkeSF8_alma1ayYMZcH9VSMLrmhAmikrDyshUcg")
    }

    private fun assertFixedIpfsUrl(url: String, expectedPath: String) {
        val result = resolvePublicHttpUrl(url)
        assertThat(result).isEqualTo("$IPFS_PUBLIC_GATEWAY/ipfs/$expectedPath")
    }

    private fun assertOriginalIpfsUrl(url: String, expectedPath: String? = null) {
        val expected = expectedPath ?: url // in most cases we expect URL not changed
        val result = resolvePublicHttpUrl(url)
        assertThat(result).isEqualTo(expected)
    }

    private fun resolvePublicHttpUrl(url: String): String {
        val urlResource = urlResourceProcessor.parse(url)
        return urlResolver.resolvePublicLink(urlResource!!)
    }

    private fun resolveInnerHttpUrl(url: String): String {
        val urlResource = urlResourceProcessor.parse(url)
        return urlResolver.resolveInnerLink(urlResource!!)
    }
}

class UnsupportedResource(override val original: String) : UrlResource()
