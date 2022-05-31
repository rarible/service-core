package com.rarible.core.meta.resource.parser

import com.rarible.core.meta.resource.ResourceTestData.CID
import com.rarible.core.meta.resource.ResourceTestData.INVALID_CID
import com.rarible.core.meta.resource.Cid
import com.rarible.core.meta.resource.cid.CidV1Validator
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class CidUrlResourceParserTest {

    private val cidV1Validator = CidV1Validator()
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
            Cid(
                original = CID,
                cid = CID,
                subPath = null
            )
        )
    }

    @Test
    fun `Just valid CID and additional path`() {
        assertThat(cidUrlResourceParser.parse("$CID/5103.json")).isEqualTo(
            Cid(
                original = "$CID/5103.json",
                cid = CID,
                subPath = "/5103.json"
            )
        )
    }
}
