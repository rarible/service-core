package com.rarible.core.cache

import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.data.mongodb.repository.config.EnableReactiveMongoRepositories
import org.springframework.scheduling.annotation.EnableScheduling

@Configuration
@EnableScheduling
@EnableReactiveMongoRepositories(basePackageClasses = [CacheConfiguration::class])
@ComponentScan(basePackageClasses = [CacheConfiguration::class])
class CacheConfiguration