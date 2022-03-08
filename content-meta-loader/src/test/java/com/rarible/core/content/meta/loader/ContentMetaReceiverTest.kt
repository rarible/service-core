package com.rarible.core.content.meta.loader

import com.drew.imaging.png.PngChunkReader
import com.drew.lang.StreamReader
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.io.File
import java.net.URL

@Disabled
class ContentMetaReceiverTest {

    private val contentReceiverMetrics = ContentReceiverMetrics(SimpleMeterRegistry())
    private val contentReceiver = KtorApacheClientContentReceiver(
        timeout = 10000
    )

    private val service = ContentMetaReceiver(
        contentReceiver = contentReceiver,
        maxBytes = 128 * 1024,
        contentReceiverMetrics = contentReceiverMetrics
    )

    @Test
    fun svg() {
        val meta = getContentMeta("https://storage.opensea.io/files/73df4a40af3cd70ca6800dadc493fc2c.svg")
        assertEquals(
            ContentMeta(
                type = "image/svg+xml",
                width = 192,
                height = 192,
                size = 350
            ),
            meta
        )
        assertEquals(350, contentReceiverMetrics.totalBytesReceived)
    }

    @Test
    fun gif() {
        val meta =
            getContentMeta("https://lh3.googleusercontent.com/CIKzsJLHKmoC8YmHt3l6h7pzj-mJx5uHrS231VE006DCZ-IQLyONCtMBCYiOwbT9SzS5IdkSOF517Zq1CejmHVrMuQ=s250")
        assertEquals(
            ContentMeta(
                type = "image/gif",
                width = 165,
                height = 250,
                size = 1570431
            ),
            meta
        )
    }

    @Test
    fun mp4() {
        val meta = getContentMeta("https://storage.opensea.io/files/3f89eab5930c7b61acb22a45412f1662.mp4")
        assertEquals(
            ContentMeta(
                type = "video/mp4",
                width = null,
                height = null,
                size = 4996096
            ),
            meta
        )
    }

    @Test
    fun amazon() {
        val meta =
            getContentMeta("https://s3.us-west-2.amazonaws.com/sing.serve/e487c504da821859cbac142e63ef9d8cc36015f0dfaf1de2949e6f894f5aa538%2Feae9b612-df09-4023-9b53-ac73e6319b44")
        assertEquals(
            ContentMeta(
                type = "video/mp4",
                width = 1280,
                height = 700,
                size = 43091297
            ),
            meta
        )
    }

    @Test
    fun jpeg() {
        val meta = getContentMeta(
            "https://lh3.googleusercontent.com/rnS-RmufKkrLlWb4gl0_3yHx_lsQI7V0kRbB1VAiSCBRcY-fiHa_2U42xexLz9ZtaUZnRuo2-o-CcYPuCkmVdko=s250"
        )
        assertEquals(
            ContentMeta(
                type = "image/jpeg",
                width = 167,
                height = 250,
                size = 44789
            ),
            meta
        )
    }

    @Test
    fun video() {
        val meta = getContentMeta(
            "https://ipfs.io/ipfs/QmSNhGhcBynr1s9QgPnon8HaiPzE5dKgmqSDNsNXCfDHGs/image.gif"
        )
        assertEquals(ContentMeta(type = "image/gif", width = 600, height = 404, size = 2559234), meta)
    }

    @Test
    fun png() {
        val meta = getContentMeta(
            "https://rarible.mypinata.cloud/ipfs/QmSorbC4UvLA6s92myE7CMog9htep9J5TXi4mUTFckc4mU"
        )
        assertEquals(
            ContentMeta(
                type = "image/png",
                width = 1262,
                height = 1262,
                size = 605891
            ),
            meta
        )
    }

    @Test
    fun `png with wrong content type`() {
        val meta = getContentMeta(
            "https://rinkeby.traitsy.com/meta/0xc91741d26b851d6724cffdf9aa3cf379b678272a/99362971277997261421968536521162276234322138208043033076209335008158078363510/revealed.png"
        )
        assertEquals(
            ContentMeta(
                type = "image/png",
                width = 4000,
                height = 4000,
                size = null
            ),
            meta
        )
    }

    @Test
    fun `ignore html`() {
        val meta = getContentMeta(
            "https://developer.mozilla.org/en-US/docs/Web/HTTP/Basics_of_HTTP/MIME_types/Common_types"
        )
        assertNull(meta)
    }

    @Test
    fun `ignore 404`() {
        val meta = getContentMeta(
            "https://developer.mozilla.org/en-US/docs/Web/HTTP/Basics_of_HTTP/MIME_types/NON_EXISTING"
        )
        assertNull(meta)
    }

    private fun getContentMeta(url: String): ContentMeta? = runBlocking { service.receive(url) }

}
