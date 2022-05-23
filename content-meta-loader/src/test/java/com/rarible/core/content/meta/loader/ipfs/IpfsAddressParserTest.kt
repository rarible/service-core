package com.rarible.core.content.meta.loader.ipfs

import com.rarible.core.content.meta.loader.addressing.cid.CidOneValidator
import com.rarible.core.content.meta.loader.addressing.ConstantGatewayProvider
import com.rarible.core.content.meta.loader.addressing.RandomGatewayProvider
import com.rarible.core.content.meta.loader.addressing.parser.AddressParserProvider
import com.rarible.core.content.meta.loader.addressing.parser.AddressParsingProcessor
import com.rarible.core.content.meta.loader.addressing.parser.ArweaveAddressParser
import com.rarible.core.content.meta.loader.addressing.parser.HttpAddressParser
import com.rarible.core.content.meta.loader.addressing.parser.RawCidAddressParser
import com.rarible.core.content.meta.loader.addressing.parser.ipfs.AbstractIpfsAddressParser
import com.rarible.core.content.meta.loader.addressing.parser.ipfs.ForeignIpfsUrlAddressParser
import com.rarible.core.content.meta.loader.addressing.resolver.ArweaveGatewayResolver
import com.rarible.core.content.meta.loader.addressing.resolver.GatewayResolveHandler
import com.rarible.core.content.meta.loader.addressing.resolver.IpfsGatewayResolver
import com.rarible.core.content.meta.loader.addressing.resolver.RawCidGatewayResolver
import com.rarible.core.content.meta.loader.addressing.resolver.SimpleHttpGatewayResolver
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class IpfsAddressParserTest {

    private val cidOneValidator = CidOneValidator()
    private val foreignIpfsUrlAddressParser = ForeignIpfsUrlAddressParser(
        cidOneValidator = cidOneValidator
    )

    private val ipfsGatewayResolver = IpfsGatewayResolver(
        customGatewaysResolver = RandomGatewayProvider(listOf(IPFS_CUSTOM_GATEWAY)),
        publicGatewayProvider = ConstantGatewayProvider(IPFS_PUBLIC_GATEWAY),
        innerGatewaysProvider = RandomGatewayProvider(listOf(IPFS_PUBLIC_GATEWAY))
    )

    private val addressParserProvider = AddressParserProvider(
        arweaveUrlParser = ArweaveAddressParser(),
        foreignIpfsUrlAddressParser = foreignIpfsUrlAddressParser,
        abstractIpfsAddressParser = AbstractIpfsAddressParser(),
        rawCidAddressParser = RawCidAddressParser(cidOneValidator),
        httpUrlParser = HttpAddressParser()
    )

    private val addressParsingProcessor = AddressParsingProcessor(
        addressParserProvider = addressParserProvider
    )

    private val gatewayResolveHandler = GatewayResolveHandler(
        ipfsGatewayResolver = ipfsGatewayResolver,
        rawCidGatewayResolver = RawCidGatewayResolver(
            publicGatewayProvider = ConstantGatewayProvider(IPFS_PUBLIC_GATEWAY),
            innerGatewaysProvider = RandomGatewayProvider(listOf(IPFS_PUBLIC_GATEWAY))
        ),
        arweaveGatewayResolver = ArweaveGatewayResolver(),
        simpleHttpGatewayResolver = SimpleHttpGatewayResolver()
    )

//    @Test
//    fun `svg file with CID urls`() {
//        val svg = "<svg url=https://ipfs.io/ipfs/QmQzqPpcBFkc9AwV4B2tscLy9dBwN7o9yEHE9aRCHeN6KW></svg>"
//        val result = container.resolvePublicHttpUrl(svg)
//        // should stay as SVG
//        assertThat(result).isEqualTo(svg)
//    }

    // TODO Test for Arveawe

    @Test
    fun `foreign ipfs urls - replaced by public gateway`() {
        // Broken IPFS URL
        assertFixedIpfsUrl("htt://mypinata.com/ipfs/$CID", CID)
        // Relative IPFS path
//        assertFixedIpfsUrl("/ipfs/$CID/abc .png", "$CID/abc%20.png")   //SPACE

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
        val result = resolveInnerHttpUrl("https://dweb.link/ipfs/${CID}/1.png")
        assertThat(result)
            .isEqualTo("${IPFS_PUBLIC_GATEWAY}/ipfs/${CID}/1.png")
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
    fun `some ipfs path`() {
        val path = "///hel lo.png"
//        assertThat(container.resolvePublicHttpUrl(path))              //SPACE
//            .isEqualTo("${IPFS_PUBLIC_GATEWAY}/hel%20lo.png")
    }

    @Test
    fun `replace legacy`() {
        assertThat(resolveInnerHttpUrl("${IPFS_CUSTOM_GATEWAY}/ipfs/$CID"))
            .isEqualTo("${IPFS_PUBLIC_GATEWAY}/ipfs/$CID")
    }

    private fun assertFixedIpfsUrl(url: String, expectedPath: String) {
        val result = resolvePublicHttpUrl(url)
        assertThat(result).isEqualTo("${IPFS_PUBLIC_GATEWAY}/ipfs/$expectedPath")
    }

    private fun assertOriginalIpfsUrl(url: String, expectedPath: String? = null) {
        val expected = expectedPath ?: url // in most cases we expect URL not changed
        val result = resolvePublicHttpUrl(url)
        assertThat(result).isEqualTo(expected)
    }

    private fun resolvePublicHttpUrl(url: String): String {
        val resourceAddress = addressParsingProcessor.parse(url)
        return gatewayResolveHandler.resolvePublicAddress(resourceAddress!!)
    }

    private fun resolveInnerHttpUrl(url: String): String {
        val resourceAddress = addressParsingProcessor.parse(url)
        return gatewayResolveHandler.resolveInnerAddress(resourceAddress!!)
    }

    companion object {
        const val IPFS_PUBLIC_GATEWAY = "https://ipfs.io"
        private const val IPFS_CUSTOM_GATEWAY = "https://rarible.mypinata.com" // Legacy
        private const val CID = "QmbpJhWFiwzNu7MebvKG3hrYiyWmSiz5dTUYMQLXsjT9vw"
    }
}
