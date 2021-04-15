package com.rarible.core.mongo.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rarible.core.mongo.converter.CustomMongoConverter;
import com.rarible.core.mongo.converter.ObjectNodeMongoConverter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnBean(ObjectMapper.class)
public class MongoConvertersAutoConfiguration {
    @Bean
    public CustomMongoConverter objectNodeMongoConverter(ObjectMapper mapper) {
        return new ObjectNodeMongoConverter(mapper);
    }
}
