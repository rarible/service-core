package com.rarible.core.apm.annotation.autoconfigure

import com.rarible.core.apm.annotation.SpanAnnotationPostProcessor
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean

@ConditionalOnProperty(
    prefix = RARIBLE_APM_ANNOTATION,
    name = ["enabled"],
    havingValue = "true",
    matchIfMissing = false
)
@EnableConfigurationProperties(ApmAnnotationProperties::class)
class ApmAnnotationPostProcessorConfiguration {
    @Bean
    fun spanAnnotationPostProcessor(): SpanAnnotationPostProcessor {
        return SpanAnnotationPostProcessor()
    }
}
