package com.rarible.core.telemetry.environment

import com.rarible.core.application.ApplicationEnvironmentInfo
import com.rarible.core.application.ApplicationInfo
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import java.net.InetAddress
import java.net.UnknownHostException

@EnableConfigurationProperties(ApplicationProperties::class)
internal class EnvironmentAutoConfiguration(
    private val properties: ApplicationProperties
) {
    @Value("\${spring.application.name}")
    private lateinit var applicationName: String

    @Bean
    @ConditionalOnMissingBean(ApplicationEnvironmentInfo::class)
    fun applicationEnvironmentInfo(): ApplicationEnvironmentInfo {
        val host = try {
            InetAddress.getLocalHost().hostName
        } catch (e: UnknownHostException) {
            throw RuntimeException("Failed to get local hostname. Please check if it is listed in /etc/hosts", e)
        }
        return ApplicationEnvironmentInfo(
            name = properties.environment,
            host = host
        )
    }

    @Bean
    @ConditionalOnMissingBean(ApplicationInfo::class)
    fun applicationInfo(): ApplicationInfo {
        return ApplicationInfo(
            serviceName = properties.serviceName ?: applicationName,
            project = properties.project
        )
    }
}
