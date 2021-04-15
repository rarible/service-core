package com.rarible.core.mongo.converter;

import org.springframework.core.convert.converter.Converter;

import java.util.List;

import static java.util.Arrays.asList;

public interface SimpleMongoConverter<M, D> extends CustomMongoConverter {
    Converter<M, D> getFromMongoConverter();
    Converter<D, M> getToMongoConverter();

    @Override
    default List<Converter> getConverters() {
        return asList(getFromMongoConverter(), getToMongoConverter());
    }
}
