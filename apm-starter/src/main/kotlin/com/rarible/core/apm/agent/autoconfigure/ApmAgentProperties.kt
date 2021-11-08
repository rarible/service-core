package com.rarible.core.apm.agent.autoconfigure

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding
import java.net.URI

internal const val RARIBLE_APM_AGENT = "rarible.core.apm.agent"

@ConstructorBinding
@ConfigurationProperties(RARIBLE_APM_AGENT)
data class ApmAgentProperties(
    val enabled: Boolean = false,
    val server: URI = URI.create("http://apm-server:8200"),
    val packages: String? = null,
    val instrument: Boolean = false
)
