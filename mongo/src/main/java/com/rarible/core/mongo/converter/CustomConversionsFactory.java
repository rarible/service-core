package com.rarible.core.mongo.converter;

import com.rarible.core.mongo.decimal128.Decimal128ToDoubleConverter;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class CustomConversionsFactory {
    private final List<CustomMongoConverter> customMongoConverters;

    public CustomConversionsFactory(List<CustomMongoConverter> customMongoConverters) {
        this.customMongoConverters = customMongoConverters;
    }

    public MongoCustomConversions create() {
        List<Object> converters = customMongoConverters.stream()
            .flatMap(cc -> cc.getConverters().stream())
            .collect(Collectors.toList());
        converters.add(new Decimal128ToDoubleConverter());
        converters.add(new BsonUndefinedToNullObjectConverterFactory());
        return new MongoCustomConversions(converters) {
            @Override
            public boolean isSimpleType(Class<?> type) {
                return customMongoConverters.stream().anyMatch(cc -> cc.isSimpleType(type)) || super.isSimpleType(type);
            }

            @Override
            public Optional<Class<?>> getCustomWriteTarget(Class<?> sourceType) {
                for (CustomMongoConverter converter : customMongoConverters) {
                    Optional<Class<?>> result = converter.getCustomWriteTarget(sourceType);
                    if (result.isPresent()) {
                        return result;
                    }
                }
                return super.getCustomWriteTarget(sourceType);
            }
        };
    }
}
