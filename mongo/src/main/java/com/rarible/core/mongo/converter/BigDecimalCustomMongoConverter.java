package com.rarible.core.mongo.converter;

import com.rarible.core.mongo.decimal128.BigDecimalToDecimal128Converter;
import com.rarible.core.mongo.decimal128.Decimal128ToBigDecimalConverter;
import org.bson.types.Decimal128;
import org.springframework.core.convert.converter.Converter;

import java.math.BigDecimal;
import java.util.Optional;

public class BigDecimalCustomMongoConverter implements SimpleMongoConverter<Decimal128, BigDecimal> {
    @Override
    public Converter<Decimal128, BigDecimal> getFromMongoConverter() {
        return new Decimal128ToBigDecimalConverter();
    }

    @Override
    public Converter<BigDecimal, Decimal128> getToMongoConverter() {
        return new BigDecimalToDecimal128Converter();
    }

    @Override
    public boolean isSimpleType(Class<?> aClass) {
        return Decimal128.class == aClass;
    }

    @Override
    public Optional<Class<?>> getCustomWriteTarget(Class<?> sourceType) {
        if (sourceType == BigDecimal.class) {
            return Optional.of(Decimal128.class);
        }
        return Optional.empty();
    }
}
