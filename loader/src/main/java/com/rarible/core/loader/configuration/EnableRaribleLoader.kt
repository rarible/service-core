package com.rarible.core.loader.configuration

import com.rarible.core.loader.internal.common.LoadCommonConfiguration
import com.rarible.core.loader.internal.runner.LoadRunnerConfiguration
import org.springframework.context.annotation.Import

/**
 * Enable auto-configuration for the loader infrastructure.
 *
 * Clients need to configure [LoadProperties].
 * If the [LoadProperties.enableWorkers] is `true`,
 * this application will be a runner to execute the scheduled tasks.
 * Otherwise, this application can only schedule tasks for execution by other runners.
 */
@Target(AnnotationTarget.ANNOTATION_CLASS, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@Import(
    LoadCommonConfiguration::class,
    LoadRunnerConfiguration::class
)
annotation class EnableRaribleLoader
