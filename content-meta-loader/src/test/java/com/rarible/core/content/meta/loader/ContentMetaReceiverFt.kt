package com.rarible.core.content.meta.loader

import com.rarible.core.meta.resource.detector.ContentDetector
import com.rarible.core.meta.resource.model.ContentMeta
import com.rarible.core.meta.resource.model.MimeType
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import java.net.URI

@Disabled
class ContentMetaReceiverFt {

    companion object {

        private const val MAX_BYTES = 128 * 1024

        private val contentApacheAsyncHttpContentReceiver = ApacheHttpContentReceiver(
            timeout = 10000,
            connectionsPerRoute = 200,
            keepAlive = true,
            insecure = false,
            meterRegistry = SimpleMeterRegistry(),
            monitoredUrls = emptyList(),
        )

        private val contentDetector = ContentDetector()

        private val contentMetaApacheAsyncHttpReceiver = ContentMetaReceiver(
            contentReceiver = contentApacheAsyncHttpContentReceiver,
            maxBytes = MAX_BYTES,
            contentDetector = contentDetector
        )
    }

    enum class ContentMetaReceiversEnum(val receiver: ContentMetaReceiver) {
        APACHE_ASYNC(contentMetaApacheAsyncHttpReceiver)
    }

    @ParameterizedTest
    @EnumSource(ContentMetaReceiversEnum::class)
    fun svg(receiverEnum: ContentMetaReceiversEnum) {
        val result = getContentMeta(
            "https://dev.w3.org/SVG/tools/svgweb/samples/svg-files/aa.svg",
            receiverEnum.receiver
        )

        val expectedContentMeta = ContentMeta(
            mimeType = MimeType.SVG_XML_IMAGE.value,
            width = 192,
            height = 192,
            size = 993,
            available = true
        )

        assertThat(result.meta).isEqualTo(expectedContentMeta)
        assertThat(result.approach).isEqualTo("exif")
        assertThat(result.bytesRead).isEqualTo(expectedContentMeta.size!!.toLong())
    }

    @ParameterizedTest
    @EnumSource(ContentMetaReceiversEnum::class)
    fun gif(receiverEnum: ContentMetaReceiversEnum) {
        val result = getContentMeta(
            "https://lh3.googleusercontent.com/CIKzsJLHKmoC8YmHt3l6h7pzj-mJx5uHrS231VE006DCZ-IQLyONCtMBCYiOwbT9SzS5IdkSOF517Zq1CejmHVrMuQ=s250",
            receiverEnum.receiver
        )

        val expectedContentMeta = ContentMeta(
            mimeType = MimeType.GIF_IMAGE.value,
            width = 165,
            height = 250,
            size = 1570431,
            available = true
        )

        assertThat(result.meta).isEqualTo(expectedContentMeta)
        assertThat(result.approach).isEqualTo("exif")
        assertThat(result.bytesRead).isEqualTo(MAX_BYTES)
    }

    @ParameterizedTest
    @EnumSource(ContentMetaReceiversEnum::class)
    fun mp3(receiverEnum: ContentMetaReceiversEnum) {
        val result = getContentMeta(
            "http://localhost:8080/music.mp3",
            receiverEnum.receiver
        )

        val expectedContentMeta = ContentMeta(
            mimeType = MimeType.MP3_AUDIO.value,
            available = true
        )

        assertThat(result.meta).isEqualTo(expectedContentMeta)
        assertThat(result.approach).isEqualTo("predefined")
        assertThat(result.bytesRead).isEqualTo(0)
    }

    @ParameterizedTest
    @EnumSource(ContentMetaReceiversEnum::class)
    fun obj(receiverEnum: ContentMetaReceiversEnum) {
        val result = getContentMeta(
            "http://localhost:8080/file.obj",
            receiverEnum.receiver
        )

        val expectedContentMeta = ContentMeta(
            mimeType = MimeType.OBJ_MODEL.value,
            available = true
        )

        assertThat(result.meta).isEqualTo(expectedContentMeta)
        assertThat(result.approach).isEqualTo("predefined")
        assertThat(result.bytesRead).isEqualTo(0)
    }

    @ParameterizedTest
    @EnumSource(ContentMetaReceiversEnum::class)
    fun mp4(receiverEnum: ContentMetaReceiversEnum) {
        val result = getContentMeta(
            "https://www.learningcontainer.com/download/sample-mp4-video-file-download-for-testing/?wpdmdl=2727&refresh=62810df6e03441652624886",
            receiverEnum.receiver
        )

        val expectedContentMeta = ContentMeta(
            mimeType = MimeType.MP4_VIDEO.value,
            width = 320,
            height = 240,
            size = null,
            available = true
        )

        assertThat(result.meta).isEqualTo(expectedContentMeta)
        assertThat(result.approach).isEqualTo("exif")
        assertThat(result.bytesRead).isEqualTo(MAX_BYTES)
    }

    @ParameterizedTest
    @EnumSource(ContentMetaReceiversEnum::class)
    fun amazon(receiverEnum: ContentMetaReceiversEnum) {
        val result = getContentMeta(
            "https://s3.us-west-2.amazonaws.com/sing.serve/e487c504da821859cbac142e63ef9d8cc36015f0dfaf1de2949e6f894f5aa538%2Feae9b612-df09-4023-9b53-ac73e6319b44",
            receiverEnum.receiver
        )

        val expectedContentMeta = ContentMeta(
            mimeType = MimeType.MP4_VIDEO.value,
            width = 1280,
            height = 700,
            size = 43091297,
            available = true
        )

        assertThat(result.meta).isEqualTo(expectedContentMeta)
        assertThat(result.approach).isEqualTo("exif")
        assertThat(result.bytesRead).isEqualTo(MAX_BYTES)
    }

    @ParameterizedTest
    @EnumSource(ContentMetaReceiversEnum::class)
    fun jpeg(receiverEnum: ContentMetaReceiversEnum) {
        val result = getContentMeta(
            "https://lh3.googleusercontent.com/rnS-RmufKkrLlWb4gl0_3yHx_lsQI7V0kRbB1VAiSCBRcY-fiHa_2U42xexLz9ZtaUZnRuo2-o-CcYPuCkmVdko=s250",
            receiverEnum.receiver
        )

        val expectedContentMeta = ContentMeta(
            mimeType = MimeType.JPEG_IMAGE.value,
            width = 167,
            height = 250,
            size = 44789,
            available = true
        )

        assertThat(result.meta).isEqualTo(expectedContentMeta)
        assertThat(result.approach).isEqualTo("exif")
        assertThat(result.bytesRead).isEqualTo(expectedContentMeta.size!!.toLong())
    }

    @ParameterizedTest
    @EnumSource(ContentMetaReceiversEnum::class)
    fun video(receiverEnum: ContentMetaReceiversEnum) {
        val result = getContentMeta(
            "https://ipfs.io/ipfs/QmSNhGhcBynr1s9QgPnon8HaiPzE5dKgmqSDNsNXCfDHGs/image.gif", receiverEnum.receiver
        )

        val expectedContentMeta = ContentMeta(
            mimeType = MimeType.GIF_IMAGE.value,
            width = 600,
            height = 404,
            size = 2559234,
            available = true
        )

        assertThat(result.meta).isEqualTo(expectedContentMeta)
        assertThat(result.approach).isEqualTo("exif")
        assertThat(result.bytesRead).isEqualTo(MAX_BYTES)
    }

    @ParameterizedTest
    @EnumSource(ContentMetaReceiversEnum::class)
    fun png(receiverEnum: ContentMetaReceiversEnum) {
        val result = getContentMeta(
            "https://lh3.googleusercontent.com/v-6yD0Vf2BEo-nSPG-VuSSdYYAxaJkgFAAdizbO_2gxgqa85eWg0l27lerLKxOOcfJjKf7bCmug3S_cbJdCQ-csxqLN_Fvs3vHVOZFU",
            receiverEnum.receiver
        )

        val expectedContentMeta = ContentMeta(
            mimeType = MimeType.PNG_IMAGE.value,
            width = 512,
            height = 512,
            size = 173580,
            available = true
        )

        assertThat(result.meta).isEqualTo(expectedContentMeta)
        assertThat(result.approach).isEqualTo("exif")
        assertThat(result.bytesRead).isEqualTo(MAX_BYTES)
    }

    @ParameterizedTest
    @EnumSource(ContentMetaReceiversEnum::class)
    fun `png - by url`(receiverEnum: ContentMetaReceiversEnum) {
        val result = getContentMeta(
            "https://localhost:8080/image.png",
            receiverEnum.receiver
        )

        val expectedContentMeta = ContentMeta(
            mimeType = MimeType.PNG_IMAGE.value,
            available = false
        )

        assertThat(result.meta).isEqualTo(expectedContentMeta)
        assertThat(result.approach).isEqualTo("url-extension")
        assertThat(result.bytesRead).isEqualTo(0)
    }

    @ParameterizedTest
    @EnumSource(ContentMetaReceiversEnum::class)
    fun html(receiverEnum: ContentMetaReceiversEnum) {
        val result = getContentMeta(
            "https://lens.mypinata.cloud/ipfs/QmPvbSDFp2rppbv5pP1i4eoqEoioH3rhH5NAtgnAZskLFh/?handle=marinawilde2.lens",
            receiverEnum.receiver
        )

        val expectedContentMeta = ContentMeta(
            mimeType = MimeType.HTML_TEXT.value,
            available = true,
            size = 22560
        )

        assertThat(result.meta).isEqualTo(expectedContentMeta)
        assertThat(result.approach).isEqualTo("exif")
        assertThat(result.bytesRead).isEqualTo(expectedContentMeta.size!!.toLong())
    }

    @ParameterizedTest
    @EnumSource(ContentMetaReceiversEnum::class)
    fun `ignore 404`(receiverEnum: ContentMetaReceiversEnum) {
        val result = getContentMeta(
            "https://developer.mozilla.org/en-US/docs/Web/HTTP/Basics_of_HTTP/MIME_types/NON_EXISTING",
            receiverEnum.receiver
        )
        assertNull(result.meta)
        assertThat(result.approach).isEqualTo("stub")
        assertThat(result.bytesRead).isEqualTo(0)
    }

    @Test
    fun `allow insecure`() = runBlocking<Unit> {
        val body = String(
            ApacheHttpContentReceiver(
                connectionsPerRoute = 1,
                insecure = true,
                keepAlive = false,
                timeout = 30,
                meterRegistry = SimpleMeterRegistry(),
                monitoredUrls = emptyList(),
            ).receiveBytes("ethereum", URI("https://chameleoncollective.io/metadata/9277.json"), 1000000).data
        )

        assertThat(body).isEqualTo("""{"image":"https://chameleoncollective.io/metadata2/1176.png","attributes":[{"value":"Blue Veins","trait_type":"Eye"},{"value":"Red","trait_type":"Background"},{"value":"Green Moustache","trait_type":"Prop"},{"value":"Tangled White","trait_type":"Mouth"},{"value":"White Hoodie","trait_type":"Clothes"},{"value":"Basketball Green","trait_type":"Tail"},{"value":"Cowboy Brown","trait_type":"Hat"},{"value":"Green","trait_type":"Body"}]}""")
    }

    private fun getContentMeta(url: String, contentMetaReceiver: ContentMetaReceiver): ContentMetaResult =
        runBlocking { contentMetaReceiver.receive("ethereum", URI(url)) }
}
