package com.rarible.core.meta.resource.cid

interface CidValidator {
    fun isCid(test: String): Boolean
}
