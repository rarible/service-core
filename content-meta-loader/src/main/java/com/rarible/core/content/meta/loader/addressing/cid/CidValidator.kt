package com.rarible.core.content.meta.loader.addressing.cid

interface CidValidator {
    fun isCid(test: String): Boolean
}
