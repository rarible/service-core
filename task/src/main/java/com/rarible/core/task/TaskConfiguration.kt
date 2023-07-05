package com.rarible.core.task

import io.micrometer.core.instrument.MeterRegistry
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.data.mongodb.repository.config.EnableReactiveMongoRepositories
import org.springframework.scheduling.annotation.EnableScheduling

@Configuration
@EnableScheduling
@EnableReactiveMongoRepositories(basePackageClasses = [TaskConfiguration::class])
@ComponentScan(basePackageClasses = [TaskConfiguration::class])
@EnableConfigurationProperties(value = [RaribleTaskProperties::class])
class TaskConfiguration {

    @Bean
    fun raribleTaskWorker(
        taskService: TaskService,
        properties: RaribleTaskProperties,
        meterRegistry: MeterRegistry,
    ): RaribleTaskWorker {
        return RaribleTaskWorker(taskService, properties, meterRegistry)
    }
}