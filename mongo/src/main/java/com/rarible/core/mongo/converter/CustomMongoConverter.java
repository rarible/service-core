package com.rarible.core.mongo.converter;

import org.springframework.core.convert.converter.Converter;

import java.util.List;
import java.util.Optional;

public interface CustomMongoConverter {
    List<Converter> getConverters();
    boolean isSimpleType(Class<?> aClass);
    Optional<Class<?>> getCustomWriteTarget(Class<?> sourceType);
}
