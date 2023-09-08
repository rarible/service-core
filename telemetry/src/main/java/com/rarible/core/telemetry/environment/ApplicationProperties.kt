package com.rarible.core.telemetry.environment

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding
import org.springframework.validation.annotation.Validated
import javax.validation.constraints.NotBlank
import javax.validation.constraints.NotEmpty
import javax.validation.constraints.Pattern

@Validated
@ConstructorBinding
@ConfigurationProperties("application")
data class ApplicationProperties(
    /**
     * Name of the environment the application runs in
     */
    @NotEmpty
    @Pattern(regexp = "dev|e2e|staging|prod")
    val environment: String,

    /**
     * Name of the service the application represents. Defaults to `spring.application.name`.
     */
    @NotBlank
    val serviceName: String?,

    /**
     * Name of the project the application represents. Defaults `rarible`.
     */
    val project: String = "com/rarible"
)
