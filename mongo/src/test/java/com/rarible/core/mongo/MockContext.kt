package com.rarible.core.mongo

import com.rarible.core.mongo.configuration.EnableRaribleMongo
import com.rarible.core.mongo.configuration.IncludePersistProperties
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.context.annotation.Configuration

@Configuration
@EnableRaribleMongo
@EnableAutoConfiguration
@IncludePersistProperties
class MockContext
