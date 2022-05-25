package com.rarible.core.meta.resource.detector.ethereum

import org.apache.commons.codec.binary.Base64

/**
 * Parser/Detector for URLs in meta like "https://rarible.mypinata.cloud/data:image/png;base64,iVBORw0KGgoAAAANS..."
 */
class Base64Detector(url: String) : ContentDetector(url) {

    private val markerIndex = url.indexOf(base64marker)

    override fun canDecode(): Boolean {
        return markerIndex >= 0
    }

    // Don't want to use regex here, not sure how fast it will work on large strings
    companion object {
        private const val MIME_TYPE_PREFIX = "data:"
        private const val base64marker = ";base64,"
    }

    override fun getData(): String {
        val prefixIndex = url.indexOf(base64marker)
        return url.substring(prefixIndex + base64marker.length).trim()
    }

    override fun getDecodedData(): ByteArray? {
        return Base64.decodeBase64(getData())
    }

    override fun getMimeType(): String {
        return url.substring(url.indexOf(MIME_TYPE_PREFIX) + MIME_TYPE_PREFIX.length, markerIndex).trim()
    }

    // Юнион
    // На вход строка - определить что это (урл или ебмедед картинка)
    // Получить по урле стрим байтов и определить пнг/картинка или еще что-то ()

    // Эфир - оставить только бейз64
}
