package com.rarible.core.meta.resource.parser

import com.rarible.core.meta.resource.AddressingTestData.CID
import com.rarible.core.meta.resource.AddressingTestData.INVALID_CID
import com.rarible.core.meta.resource.Cid
import com.rarible.core.meta.resource.cid.CidV1Validator
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class RawCidAddressParserTest {

    private val cidOneValidator = CidV1Validator()
    private val rawCidAddressParser = CidUrlResourceParser(
        cidOneValidator = cidOneValidator
    )

    @Test
    fun `Invalid CID`() {
        assertThat(rawCidAddressParser.parse(INVALID_CID)).isNull()
    }

    @Test
    fun `Just valid CID`() {
        assertThat(rawCidAddressParser.parse(CID)).isEqualTo(
            Cid(
                original = CID,
                cid = CID,
                additionalPath = null
            )
        )
    }

    @Test
    fun `Just valid CID and additional path`() {
        assertThat(rawCidAddressParser.parse("$CID/5103.json")).isEqualTo(
            Cid(
                original = "$CID/5103.json",
                cid = CID,
                additionalPath = "/5103.json"
            )
        )
    }
}
