package com.rarible.core.mongo.configuration;

import com.rarible.core.mongo.converter.CustomConversionsFactory;
import com.rarible.core.mongo.jackson.BigIntegerToStringSerializer;
import com.rarible.core.mongo.jackson.ObjectIdCombinedSerializer;
import com.rarible.core.mongo.migrate.MongoIndicesService;
import com.rarible.core.mongo.settings.MongoSettingsCustomizer;
import com.rarible.core.mongo.template.RaribleReactiveMongoTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.ReactiveMongoDatabaseFactory;
import org.springframework.data.mongodb.core.convert.MongoConverter;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;

@Configuration
@Import({
        ConvertersConfiguration.class,
        ObjectIdCombinedSerializer.class,
        BigIntegerToStringSerializer.class,
        MongoIndicesService.class,
        MongoSettingsCustomizer.class
})
@EnableConfigurationProperties(value = MongoProperties.class)
public class MongoConfiguration {
    private static final Logger logger = LoggerFactory.getLogger(MongoConfiguration.class);

    @Autowired
    private CustomConversionsFactory customConversionsFactory;

    @Bean
    public MongoCustomConversions customConversions() {
        return customConversionsFactory.create();
    }

    @Bean
    public RaribleReactiveMongoTemplate reactiveMongoTemplate(ReactiveMongoDatabaseFactory reactiveMongoDatabaseFactory,
                                                              MongoConverter converter,
                                                              MongoProperties properties) {
        logger.info("Mongo properties: {}", properties);
        return new RaribleReactiveMongoTemplate(
                reactiveMongoDatabaseFactory,
                converter,
                properties
        );
    }
}
