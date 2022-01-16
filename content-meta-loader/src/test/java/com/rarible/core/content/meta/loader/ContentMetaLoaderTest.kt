package com.rarible.core.content.meta.loader

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

@Disabled // Because requires the Internet connection.
class ContentMetaLoaderTest {
    private val service = ContentMetaLoader(
        mediaFetchTimeout = 5000,
        mediaFetchMaxSize = 256 * 1024L,
        openSeaProxyUrl = "http://69.197.181.202:3128"
    )

    @Test
    fun gif() {
        val meta = runBlocking {
            service.fetchContentMeta("https://lh3.googleusercontent.com/CIKzsJLHKmoC8YmHt3l6h7pzj-mJx5uHrS231VE006DCZ-IQLyONCtMBCYiOwbT9SzS5IdkSOF517Zq1CejmHVrMuQ=s250")!!
        }
        assertEquals("image/gif", meta.type)
        assertEquals(165, meta.width)
        assertEquals(250, meta.height)
        assertEquals(1570431, meta.size)
    }

    @Test
    fun mp4() {
        val meta = runBlocking {
            service.fetchContentMeta("https://storage.opensea.io/files/3f89eab5930c7b61acb22a45412f1662.mp4")!!
        }
        assertEquals("video/mp4", meta.type)
        assertEquals(null, meta.width)
        assertEquals(null, meta.height)
        assertEquals(null, meta.size)
    }

    @Test
    fun amazon() {
        val meta = runBlocking {
            service.fetchContentMeta("https://s3.us-west-2.amazonaws.com/sing.serve/e487c504da821859cbac142e63ef9d8cc36015f0dfaf1de2949e6f894f5aa538%2Feae9b612-df09-4023-9b53-ac73e6319b44")!!
        }
        assertEquals("video/mp4", meta.type)
        assertEquals(null, meta.width)
        assertEquals(null, meta.height)
        assertEquals(null, meta.size)
    }

    @Test
    fun jpeg() {
        val meta = runBlocking {
            service.fetchContentMeta("https://lh3.googleusercontent.com/rnS-RmufKkrLlWb4gl0_3yHx_lsQI7V0kRbB1VAiSCBRcY-fiHa_2U42xexLz9ZtaUZnRuo2-o-CcYPuCkmVdko=s250")
        }!!

        assertEquals("image/jpeg", meta.type)
        assertEquals(167, meta.width)
        assertEquals(250, meta.height)
        assertEquals(44789, meta.size)
    }

    @Test
    fun video() {
        val meta = runBlocking { service.fetchContentMeta("https://ipfs.io/ipfs/QmSNhGhcBynr1s9QgPnon8HaiPzE5dKgmqSDNsNXCfDHGs/image.gif")!! }
        assertEquals(ContentMeta(type = "image/gif", width = 600, height = 404, size = 2559234), meta)
    }

}
