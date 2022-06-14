package com.rarible.core.meta.resource.parser.ipfs

import com.rarible.core.meta.resource.cid.CidV1Validator
import com.rarible.core.meta.resource.model.IpfsUrl
import com.rarible.core.meta.resource.test.ResourceTestData.CID
import com.rarible.core.meta.resource.test.ResourceTestData.INVALID_CID
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class CidUrlResourceParserTest {

    private val cidV1Validator = CidV1Validator
    private val cidUrlResourceParser = CidUrlResourceParser(
        cidOneValidator = cidV1Validator
    )

    @Test
    fun `Invalid CID`() {
        assertThat(cidUrlResourceParser.parse(INVALID_CID)).isNull()
    }

    @Test
    fun `Just valid CID`() {
        assertThat(cidUrlResourceParser.parse(CID)).isEqualTo(
            IpfsUrl(
                original = CID,
                path = CID,
                originalGateway = null
            )
        )
    }

    @Test
    fun `Just valid CID and additional path`() {
        assertThat(cidUrlResourceParser.parse("$CID/5103.json")).isEqualTo(
            IpfsUrl(
                original = "$CID/5103.json",
                path = "$CID/5103.json",
                originalGateway = null
            )
        )
    }
}
