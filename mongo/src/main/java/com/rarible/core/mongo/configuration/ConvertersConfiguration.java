package com.rarible.core.mongo.configuration;

import com.rarible.core.mongo.converter.BigDecimalCustomMongoConverter;
import com.rarible.core.mongo.converter.BigIntegerCustomMongoConverter;
import com.rarible.core.mongo.converter.CustomConversionsFactory;
import com.rarible.core.mongo.converter.CustomMongoConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class ConvertersConfiguration {
    @Bean
    public CustomMongoConverter bigIntegerCustomMongoConverter() {
        return new BigIntegerCustomMongoConverter();
    }

    @Bean
    public CustomMongoConverter bigDecimalCustomMongoConverter() {
        return new BigDecimalCustomMongoConverter();
    }

    @Bean
    public CustomConversionsFactory customConversionsFactory(List<CustomMongoConverter> customMongoConverters) {
        return new CustomConversionsFactory(customMongoConverters);
    }
}
