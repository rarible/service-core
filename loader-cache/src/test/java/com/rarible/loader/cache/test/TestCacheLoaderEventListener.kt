package com.rarible.loader.cache.test

import com.rarible.loader.cache.CacheLoaderEvent
import com.rarible.loader.cache.CacheLoaderEventListener
import org.springframework.stereotype.Component
import java.util.concurrent.CopyOnWriteArrayList

const val testCacheType = "test-image"

val cacheEvents: CopyOnWriteArrayList<CacheLoaderEvent<TestImage>> = CopyOnWriteArrayList()

@Component
class TestCacheLoaderEventListener : CacheLoaderEventListener<TestImage> {
    override val type = testCacheType

    override suspend fun onEvent(cacheLoaderEvent: CacheLoaderEvent<TestImage>) {
        cacheEvents += cacheLoaderEvent
    }
}
