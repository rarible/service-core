package com.rarible.core.mongo.settings

import com.mongodb.MongoClientSettings
import com.rarible.core.mongo.configuration.MongoProperties
import org.springframework.boot.autoconfigure.mongo.MongoClientSettingsBuilderCustomizer
import org.springframework.stereotype.Component
import java.util.concurrent.TimeUnit

@Component
class MongoSettingsCustomizer(
    private val mongoProperties: MongoProperties,
) : MongoClientSettingsBuilderCustomizer {
    override fun customize(builder: MongoClientSettings.Builder) {
        builder.applyToConnectionPoolSettings {
            if (mongoProperties.maxConnectionLifeTime != null) {
                it.maxConnectionLifeTime(mongoProperties.maxConnectionLifeTime.toMillis(), TimeUnit.MILLISECONDS)
            }
            if (mongoProperties.maxConnectionIdleTime != null) {
                it.maxConnectionIdleTime(mongoProperties.maxConnectionIdleTime.toMillis(), TimeUnit.MILLISECONDS)
            }
            if (mongoProperties.poolSize != null) {
                it.maxSize(mongoProperties.poolSize)
            }
        }
    }
}
