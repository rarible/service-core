package com.rarible.core.autoconfigure.nginx

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding

internal const val RARIBLE_CORE_NGINX_EXPOSE = "rarible.core.nginx-expose"

@ConfigurationProperties(RARIBLE_CORE_NGINX_EXPOSE)
@ConstructorBinding
data class RaribleNginxExposeProperties(
    val enabled: Boolean = false,
    val server: String,
    val location: String
)