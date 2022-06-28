package com.rarible.core.meta.resource.cid

import io.ipfs.cid.Cid

object CidV1Validator : CidValidator {

    override fun isCid(test: String): Boolean =
        try {
            Cid.decode(test)
            true
        } catch (e: Exception) {
            false
        }
}
