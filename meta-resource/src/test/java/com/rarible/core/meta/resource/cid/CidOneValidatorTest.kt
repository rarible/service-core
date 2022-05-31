package com.rarible.core.meta.resource.cid

import com.rarible.core.meta.resource.ResourceTestData.CID
import com.rarible.core.meta.resource.ResourceTestData.INVALID_CID
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class CidOneValidatorTest {

    private val cidOneValidator = CidV1Validator()
    private val legacyCidValidator = CidLegacyValidator()

    @Test
    fun `is cid with cid one validator`() {
        assertThat(cidOneValidator.isCid(CID)).isTrue
        assertThat(cidOneValidator.isCid(INVALID_CID)).isFalse
        assertThat(cidOneValidator.isCid("bafybeigdyrzt5sfp7udm7hu76uh7y26nf3efuylqabf3oclgtqy55fbzdi")).isTrue
        assertThat(cidOneValidator.isCid("bafybeigdyrzt5sfp7udm7hu76uh7y26nf3efuyl%abf3oclgtqy55fbzdi")).isFalse
        assertThat(cidOneValidator.isCid("3")).isFalse
        assertThat(
            cidOneValidator.isCid("f01701220c3c4733ec8affd06cf9e9ff50ffc6bcd2ec85a6170004bb709669c31de94391a")
        ).isTrue
        // TODO Add more cases
    }

    @Test
    fun `is cid with legacy cid validator`() {
        assertThat(legacyCidValidator.isCid(CID)).isTrue
        assertThat(legacyCidValidator.isCid(INVALID_CID)).isFalse
        assertThat(legacyCidValidator.isCid("bafybeigdyrzt5sfp7udm7hu76uh7y26nf3efuylqabf3oclgtqy55fbzdi")).isTrue
        assertThat(legacyCidValidator.isCid("bafybeigdyrzt5sfp7udm7hu76uh7y26nf3efuyl%abf3oclgtqy55fbzdi")).isFalse
        assertThat(legacyCidValidator.isCid("3")).isFalse
        assertThat(legacyCidValidator.isCid("f01701220c3c4733ec8affd06cf9e9ff50ffc6bcd2ec85a6170004bb709669c31de94391a")).isTrue
    }
}
