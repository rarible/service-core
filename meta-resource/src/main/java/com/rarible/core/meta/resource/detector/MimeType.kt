package com.rarible.core.meta.resource.detector

enum class MimeType(
    val value: String
) {
    HTML_TEXT("text/html"),

    PNG_IMAGE("image/png"),
    APNG_IMAGE("image/apng"),
    SVG_XML_IMAGE("image/svg+xml"),
    JPEG_IMAGE("image/jpeg"),
    GIF_IMAGE("image/gif"),
    BMP_IMAGE("image/bmp"),
    WEBP_IMAGE("image/webp"),

    MP4_VIDEO("video/mp4"),
    WEBM_VIDEO("video/webm"),
    X_MSVIDEO_VIDEO("video/x-msvideo"),
    MPEG_VIDEO("video/mpeg"),

    MP3_AUDIO("audio/mp3"),
    WAV_AUDIO("audio/wav"),
    FLAC_AUDIO("audio/flac"),
    MPEG_AUDIO("audio/mpeg"),

    GLTF_JSON_MODEL("model/gltf+json"),
    GLTF_BINARY_MODEL("model/gltf-binary")
}
