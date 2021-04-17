package com.rarible.core.telemetry.bootstrap

import org.springframework.boot.SpringApplication
import org.springframework.boot.env.EnvironmentPostProcessor
import org.springframework.boot.env.YamlPropertySourceLoader
import org.springframework.core.env.ConfigurableEnvironment
import org.springframework.core.env.PropertiesPropertySource
import org.springframework.core.env.PropertySource
import org.springframework.core.io.ClassPathResource
import java.util.*

class DefaultBootstrapEnvironmentPostProcessor : EnvironmentPostProcessor {
    override fun postProcessEnvironment(environment: ConfigurableEnvironment, application: SpringApplication) {
        val sources = YamlPropertySourceLoader().load(
            "rarible-bootstrap-defaults",
            ClassPathResource("/com/rarible/bootstrap/bootstrap.yml")
        )
        for (source in sources.asReversed()) {
            environment.propertySources.addLast(source)
        }

        val applicationEnvironment = environment.getProperty(APPLICATION_ENVIRONMENT)
            ?: throw IllegalArgumentException("Can't get property '$APPLICATION_ENVIRONMENT' from environment")

        val rootPath = environment.getProperty(CONSUL_ROOT_PATH)

        val defaultContextProperty = Properties()
        defaultContextProperty.setProperty(CONSUL_CONFIG_DEFAULT_CONTEXT_PROPERTY, rootPath ?: applicationEnvironment)

        val propertySource: PropertySource<Map<String, Any>> =
            PropertiesPropertySource("consul-config-default-context-property", defaultContextProperty)

        environment.propertySources.addLast(propertySource)
    }

    private companion object {
        const val APPLICATION_ENVIRONMENT = "application.environment"
        const val CONSUL_ROOT_PATH = "consul.root-path"
        const val CONSUL_CONFIG_DEFAULT_CONTEXT_PROPERTY = "spring.cloud.consul.config.default-context"
    }
}
