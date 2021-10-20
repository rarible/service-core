package com.rarible.core.mongo.configuration;

import com.rarible.core.mongo.converter.CustomConversionsFactory;
import com.rarible.core.mongo.jackson.BigIntegerToStringSerializer;
import com.rarible.core.mongo.jackson.ObjectIdCombinedSerializer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;
import com.rarible.core.mongo.migrate.MongoIndicesService;

@Configuration
@Import({
    ConvertersConfiguration.class,
    ObjectIdCombinedSerializer.class,
    BigIntegerToStringSerializer.class,
    MongoIndicesService.class
})
public class MongoConfiguration {
    @Autowired
    private CustomConversionsFactory customConversionsFactory;

    @Bean
    public MongoCustomConversions customConversions() {
        return customConversionsFactory.create();
    }
}
