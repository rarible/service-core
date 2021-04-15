package com.rarible.core.mongo.converter;

import com.rarible.core.mongo.decimal128.BigIntegerToDecimal128Converter;
import com.rarible.core.mongo.decimal128.Decimal128ToBigIntegerConverter;
import org.bson.types.Decimal128;
import org.springframework.core.convert.converter.Converter;

import java.math.BigInteger;
import java.util.Optional;

public class BigIntegerCustomMongoConverter implements SimpleMongoConverter<Decimal128, BigInteger> {
    @Override
    public boolean isSimpleType(Class<?> aClass) {
        return Decimal128.class == aClass || String.class == aClass;
    }

    @Override
    public Optional<Class<?>> getCustomWriteTarget(Class<?> sourceType) {
        if (sourceType == BigInteger.class) {
            return Optional.of(String.class);
        }
        return Optional.empty();
    }

    @Override
    public Converter<Decimal128, BigInteger> getFromMongoConverter() {
        return new Decimal128ToBigIntegerConverter();
    }

    @Override
    public Converter<BigInteger, Decimal128> getToMongoConverter() {
        return new BigIntegerToDecimal128Converter();
    }
}