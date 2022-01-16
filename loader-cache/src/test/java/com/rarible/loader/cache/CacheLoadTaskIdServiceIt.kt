package com.rarible.loader.cache

import com.rarible.core.test.data.randomString
import com.rarible.loader.cache.internal.CacheLoadTaskIdService
import kotlinx.coroutines.flow.toSet
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class CacheLoadTaskIdServiceIt : AbstractIntegrationTest() {

    @Autowired
    lateinit var cacheLoadTaskIdService: CacheLoadTaskIdService

    @Test
    fun `mongo indexes`() = runBlocking<Unit> {
        val indexInfos = mongo.indexOps(CacheLoadTaskIdService.COLLECTION).indexInfo.asFlow().toSet()
        assertThat(indexInfos.map { it.name }.toSet())
            .isEqualTo(setOf("_id_", "type_1_key_1__id_1"))
    }

    @Test
    fun `save and override`() = runBlocking<Unit> {
        val type = randomString()
        val key = randomString()
        val loadTaskId1 = randomString()
        val loadTaskId2 = randomString()
        assertThat(cacheLoadTaskIdService.getLastTaskId(type, key)).isNull()
        cacheLoadTaskIdService.save(type, key, loadTaskId1)
        assertThat(cacheLoadTaskIdService.getLastTaskId(type, key)).isEqualTo(loadTaskId1)
        cacheLoadTaskIdService.save(type, key, loadTaskId2)
        assertThat(cacheLoadTaskIdService.getLastTaskId(type, key)).isEqualTo(loadTaskId2)
    }
}
