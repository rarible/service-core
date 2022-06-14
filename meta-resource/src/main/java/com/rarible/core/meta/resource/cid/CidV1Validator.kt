package com.rarible.core.meta.resource.cid

import io.ipfs.cid.Cid
import io.ipfs.cid.Cid.CidEncodingException

object CidV1Validator : CidValidator {

    override fun isCid(test: String): Boolean =
        try {
            Cid.decode(test)
            true
        } catch (e: CidEncodingException) {
            false
        } catch (e: IllegalStateException) {
            false
        }
}
