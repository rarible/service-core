package com.rarible.core.apm.agent.autoconfigure

import co.elastic.apm.attach.ElasticApmAttacher
import com.rarible.core.application.ApplicationEnvironmentInfo
import com.rarible.core.application.ApplicationInfo
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties

@ConditionalOnProperty(
    prefix = RARIBLE_APM_AGENT,
    name = ["enabled"],
    havingValue = "true",
    matchIfMissing = false
)
@EnableConfigurationProperties(ApmAgentProperties::class)
class ApmAgentConfiguration(
    properties: ApmAgentProperties,
    environmentInfo: ApplicationEnvironmentInfo,
    applicationInfo: ApplicationInfo
) {
    init {
        val server = properties.server.toASCIIString()
        val packages = properties.packages ?: error("Application package name must be defined")

        // see https://www.elastic.co/guide/en/apm/agent/java/master/config-core.html
        val settings = mapOf<String, String>(
            SERVER_URL_PROPERTY to server,
            APPLICATION_PACKAGES_PROPERTY to packages,
            SERVICE_NAME_PROPERTY to applicationInfo.serviceName,
            ENVIRONMENT_PROPERTY to environmentInfo.name,
            INSTRUMENT_PROPERTY to properties.instrument.toString(),
            TRANSACTION_SAMPLE_RATE to properties.sampling.toString()
        )
        ElasticApmAttacher.attach(settings)
    }

    companion object {
        private const val SERVER_URL_PROPERTY = "server_url"
        private const val SERVICE_NAME_PROPERTY = "service_name"
        private const val ENVIRONMENT_PROPERTY = "environment"
        private const val APPLICATION_PACKAGES_PROPERTY = "application_packages"
        private const val INSTRUMENT_PROPERTY = "instrument"
        private const val TRANSACTION_SAMPLE_RATE = "transaction_sample_rate"
    }
}
