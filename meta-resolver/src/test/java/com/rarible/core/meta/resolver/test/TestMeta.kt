package com.rarible.core.meta.resolver.test

import com.rarible.core.meta.resolver.Meta

data class TestMeta(val name: String?) : Meta {
    override fun isEmpty(): Boolean {
        return name == null
    }
}
