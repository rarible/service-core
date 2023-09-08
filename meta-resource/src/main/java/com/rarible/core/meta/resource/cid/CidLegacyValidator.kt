package com.rarible.core.meta.resource.cid

import java.util.regex.Pattern

object CidLegacyValidator : CidValidator {

    override fun isCid(test: String): Boolean {
        return CID_PATTERN.matcher(test).matches()
    }

    private val CID_PATTERN = Pattern.compile(
        "Qm[1-9A-HJ-NP-Za-km-z]{44,}|b[A-Za-z2-7]{58,}|B[A-Z2-7]{58,}|z[1-9A-HJ-NP-Za-km-z]{48,}|F[0-9A-F]{50,}|f[0-9a-f]{50,}"
    )
}
