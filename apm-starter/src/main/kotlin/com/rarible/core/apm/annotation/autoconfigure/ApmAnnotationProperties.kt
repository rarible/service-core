package com.rarible.core.apm.annotation.autoconfigure

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding

internal const val RARIBLE_APM_ANNOTATION = "rarible.core.apm.annotation"

@ConstructorBinding
@ConfigurationProperties(RARIBLE_APM_ANNOTATION)
data class ApmAnnotationProperties(
    val enabled: Boolean = true
)

