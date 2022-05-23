package com.rarible.core.content.meta.loader.addressing.cid

import io.ipfs.cid.Cid
import io.ipfs.cid.Cid.CidEncodingException

open class CidV1Validator : CidValidator {

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
