package com.rarible.core.content.meta.loader

import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource



@Disabled
class ContentMetaReceiverTest {

    companion object {
        private val contentReceiverMetrics = ContentReceiverMetrics(SimpleMeterRegistry())
        private val contentCioReceiver = KtorCioClientContentReceiver(
            timeout = 10000
        )

        private val contentApacheReceiver = KtorApacheClientContentReceiver(
            timeout = 10000
        )

        private val contentCioMetaReceiver = ContentMetaReceiver(
            contentReceiver = contentCioReceiver,
            maxBytes = 128 * 1024,
            contentReceiverMetrics = contentReceiverMetrics
        )

        private val contentMetaApacheReceiver = ContentMetaReceiver(
            contentReceiver = contentApacheReceiver,
            maxBytes = 128 * 1024,
            contentReceiverMetrics = contentReceiverMetrics
        )
    }

    enum class ContentMetaReceiversEnum(val receiver: ContentMetaReceiver){
        CIO(contentCioMetaReceiver),
        APACHE(contentMetaApacheReceiver)
    }

    @ParameterizedTest
    @EnumSource(ContentMetaReceiversEnum::class)
    fun svg(receiverEnum: ContentMetaReceiversEnum) {
        val totalBytesReceived = contentReceiverMetrics.totalBytesReceived
        val meta = getContentMeta(
            "https://storage.opensea.io/files/73df4a40af3cd70ca6800dadc493fc2c.svg",
            receiverEnum.receiver
        )
        assertEquals(
            ContentMeta(
                type = "image/svg+xml",
                width = 192,
                height = 192,
                size = 350
            ),
            meta
        )
        assertEquals(totalBytesReceived + 350, contentReceiverMetrics.totalBytesReceived)
    }

    @ParameterizedTest
    @EnumSource(ContentMetaReceiversEnum::class)
    fun gif(receiverEnum: ContentMetaReceiversEnum) {
        val meta =
            getContentMeta(
                "https://lh3.googleusercontent.com/CIKzsJLHKmoC8YmHt3l6h7pzj-mJx5uHrS231VE006DCZ-IQLyONCtMBCYiOwbT9SzS5IdkSOF517Zq1CejmHVrMuQ=s250",
                receiverEnum.receiver
            )
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

    @ParameterizedTest
    @EnumSource(ContentMetaReceiversEnum::class)
    fun mp4(receiverEnum: ContentMetaReceiversEnum) {
        val meta = getContentMeta(
            "https://storage.opensea.io/files/3f89eab5930c7b61acb22a45412f1662.mp4",
            receiverEnum.receiver
        )
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

    @ParameterizedTest
    @EnumSource(ContentMetaReceiversEnum::class)
    fun amazon(receiverEnum: ContentMetaReceiversEnum) {
        val meta =
            getContentMeta(
                "https://s3.us-west-2.amazonaws.com/sing.serve/e487c504da821859cbac142e63ef9d8cc36015f0dfaf1de2949e6f894f5aa538%2Feae9b612-df09-4023-9b53-ac73e6319b44",
                receiverEnum.receiver
            )
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

    @ParameterizedTest
    @EnumSource(ContentMetaReceiversEnum::class)
    fun jpeg(receiverEnum: ContentMetaReceiversEnum) {
        val meta = getContentMeta(
            "https://lh3.googleusercontent.com/rnS-RmufKkrLlWb4gl0_3yHx_lsQI7V0kRbB1VAiSCBRcY-fiHa_2U42xexLz9ZtaUZnRuo2-o-CcYPuCkmVdko=s250",
            receiverEnum.receiver
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

    @ParameterizedTest
    @EnumSource(ContentMetaReceiversEnum::class)
    fun video(receiverEnum: ContentMetaReceiversEnum) {
        val meta = getContentMeta(
            "https://ipfs.io/ipfs/QmSNhGhcBynr1s9QgPnon8HaiPzE5dKgmqSDNsNXCfDHGs/image.gif", receiverEnum.receiver
        )
        assertEquals(ContentMeta(type = "image/gif", width = 600, height = 404, size = 2559234), meta)
    }

    @ParameterizedTest
    @EnumSource(ContentMetaReceiversEnum::class)
    fun png(receiverEnum: ContentMetaReceiversEnum) {
        val meta = getContentMeta(
            "https://lh3.googleusercontent.com/v-6yD0Vf2BEo-nSPG-VuSSdYYAxaJkgFAAdizbO_2gxgqa85eWg0l27lerLKxOOcfJjKf7bCmug3S_cbJdCQ-csxqLN_Fvs3vHVOZFU",
            receiverEnum.receiver
        )
        assertEquals(
            ContentMeta(
                type = "image/png",
                width = 512,
                height = 512,
                size = 173580
            ),
            meta
        )
    }

    @ParameterizedTest
    @EnumSource(ContentMetaReceiversEnum::class)
    fun `png with wrong content type`(receiverEnum: ContentMetaReceiversEnum) {
        val meta = getContentMeta(
            "https://rinkeby.traitsy.com/meta/0xc91741d26b851d6724cffdf9aa3cf379b678272a/99362971277997261421968536521162276234322138208043033076209335008158078363510/revealed.png",
            receiverEnum.receiver
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

    @ParameterizedTest
    @EnumSource(ContentMetaReceiversEnum::class)
    fun `ignore html`(receiverEnum: ContentMetaReceiversEnum) {
        val meta = getContentMeta(
            "https://developer.mozilla.org/en-US/docs/Web/HTTP/Basics_of_HTTP/MIME_types/Common_types",
            receiverEnum.receiver
        )
        assertNull(meta)
    }

    @ParameterizedTest
    @EnumSource(ContentMetaReceiversEnum::class)
    fun `ignore 404`(receiverEnum: ContentMetaReceiversEnum) {
        val meta = getContentMeta(
            "https://developer.mozilla.org/en-US/docs/Web/HTTP/Basics_of_HTTP/MIME_types/NON_EXISTING",
            receiverEnum.receiver
        )
        assertNull(meta)
    }

    private fun getContentMeta(url: String, contentMetaReceiver: ContentMetaReceiver): ContentMeta? =
        runBlocking { contentMetaReceiver.receive(url) }
}
