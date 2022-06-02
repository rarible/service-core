package com.rarible.core.meta.resource.resolver

import com.rarible.core.meta.resource.HttpUrl
import com.rarible.core.meta.resource.IpfsUrl
import com.rarible.core.meta.resource.SchemaUrl
import com.rarible.core.meta.resource.UrlResource
import com.rarible.core.meta.resource.parser.UrlParser
import com.rarible.core.meta.resource.test.ResourceTestData
import com.rarible.core.meta.resource.test.ResourceTestData.CID
import com.rarible.core.meta.resource.test.ResourceTestData.IPFS_CUSTOM_GATEWAY
import com.rarible.core.meta.resource.test.ResourceTestData.IPFS_PRIVATE_GATEWAY
import com.rarible.core.meta.resource.test.ResourceTestData.IPFS_PUBLIC_GATEWAY
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class UrlResolverTest {

    private val ipfsGatewayResolver = IpfsGatewayResolver(
        publicGatewayProvider = ConstantGatewayProvider(IPFS_PUBLIC_GATEWAY),
        internalGatewayProvider = RandomGatewayProvider(listOf(IPFS_PRIVATE_GATEWAY)),
        customGatewaysResolver = LegacyIpfsGatewaySubstitutor(listOf(IPFS_CUSTOM_GATEWAY))
    )

    private val urlParser = UrlParser()
    private val urlResolver = UrlResolver(ipfsGatewayResolver)

    @Test
    fun `SimpleHttpGatewayResolver happy path`() {
        assertThat(
            urlResolver.resolvePublicUrl(
                resource = HttpUrl(original = "${ResourceTestData.ORIGINAL_GATEWAY}/ipfs/${CID}")
            )
        ).isEqualTo("${ResourceTestData.ORIGINAL_GATEWAY}/ipfs/${CID}")
    }

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
            .isEqualTo("$IPFS_PRIVATE_GATEWAY/ipfs/$CID/1.png")
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
            .isEqualTo("$IPFS_PRIVATE_GATEWAY/ipfs/$CID")
    }

    @Test
    fun `Thrown Unsupported Exception`() {
        val thrown = assertThrows(
            UnsupportedOperationException::class.java,
            { urlResolver.resolveInternalUrl(UnsupportedResource("test")) },
            "UnsupportedOperationException error was expected"
        )

        assertEquals("Unsupported resolving for com.rarible.core.meta.resource.resolver.UnsupportedResource", thrown.message)
    }

    @Test
    fun `arweave full url`() {
        assertThat(
            urlResolver.resolvePublicUrl(
                SchemaUrl(
                    original = "ar://lVS0SkeSF8_alma1ayYMZcH9VSMLrmhAmikrDyshUcg",
                    gateway = "https://arweave.net",
                    path = "lVS0SkeSF8_alma1ayYMZcH9VSMLrmhAmikrDyshUcg",
                    schema = "ar"
                )
            )
        ).isEqualTo("https://arweave.net/lVS0SkeSF8_alma1ayYMZcH9VSMLrmhAmikrDyshUcg")
    }

    @Test
    fun `RawCidGatewayResolver resolve public without additional path`() {
        assertThat(
            urlResolver.resolvePublicUrl(
                resource = IpfsUrl(
                    original = CID,
                    path = CID,
                    originalGateway = null
                )
            )
        ).isEqualTo("$IPFS_PUBLIC_GATEWAY/ipfs/$CID")
    }

    @Test
    fun `RawCidGatewayResolver resolve public with additional path`() {
        assertThat(
            urlResolver.resolvePublicUrl(
                resource = IpfsUrl(
                    original = "$CID/5032.json",
                    path = "$CID/5032.json",
                    originalGateway = null
                )
            )
        ).isEqualTo("$IPFS_PUBLIC_GATEWAY/ipfs/$CID/5032.json")
    }

    @Test
    fun `RawCidGatewayResolver resolve internal without additional path`() {
        assertThat(
            urlResolver.resolveInternalUrl(
                resource = IpfsUrl(
                    original = CID,
                    path = CID,
                    originalGateway = null
                )
            )
        ).isEqualTo("$IPFS_PRIVATE_GATEWAY/ipfs/$CID")
    }

    @Test
    fun `RawCidGatewayResolver resolve internal with additional path`() {
        assertThat(
            urlResolver.resolveInternalUrl(
                resource = IpfsUrl(
                    original = "$CID/5032.json",
                    path = "$CID/5032.json",
                    originalGateway = null
                )
            )
        ).isEqualTo("$IPFS_PRIVATE_GATEWAY/ipfs/$CID/5032.json")
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
        val urlResource = urlParser.parse(url)
        return urlResolver.resolvePublicUrl(urlResource!!)
    }

    private fun resolveInnerHttpUrl(url: String): String {
        val urlResource = urlParser.parse(url)
        return urlResolver.resolveInternalUrl(urlResource!!)
    }
}

class UnsupportedResource(override val original: String) : UrlResource()
