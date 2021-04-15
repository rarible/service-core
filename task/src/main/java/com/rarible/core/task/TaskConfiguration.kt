package com.rarible.core.task

import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.data.mongodb.repository.config.EnableReactiveMongoRepositories
import org.springframework.scheduling.annotation.EnableScheduling

@Configuration
@EnableScheduling
@EnableReactiveMongoRepositories(basePackageClasses = [TaskConfiguration::class])
@ComponentScan(basePackageClasses = [TaskConfiguration::class])
class TaskConfiguration