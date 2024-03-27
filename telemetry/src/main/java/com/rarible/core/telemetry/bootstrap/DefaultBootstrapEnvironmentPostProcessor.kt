package com.rarible.core.telemetry.bootstrap

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.boot.SpringApplication
import org.springframework.boot.env.EnvironmentPostProcessor
import org.springframework.boot.env.YamlPropertySourceLoader
import org.springframework.core.env.ConfigurableEnvironment
import org.springframework.core.io.ClassPathResource

class DefaultBootstrapEnvironmentPostProcessor : EnvironmentPostProcessor {
    override fun postProcessEnvironment(environment: ConfigurableEnvironment, application: SpringApplication) {
        logger.info("postProcessEnvironment")
        val sources = YamlPropertySourceLoader().load(
            "rarible-bootstrap-defaults",
            ClassPathResource("/com/rarible/bootstrap/bootstrap.yml")
        )
        for (source in sources.asReversed()) {
            environment.propertySources.addLast(source)
        }
    }

    private companion object {
        val logger: Logger = LoggerFactory.getLogger(DefaultBootstrapEnvironmentPostProcessor::class.java)
    }
}
