package com.rarible.core.mongo.decimal128;

import org.bson.types.Decimal128;
import org.springframework.core.convert.converter.Converter;

import java.math.BigInteger;

public class Decimal128ToBigIntegerConverter implements Converter<Decimal128, BigInteger> {
    @Override
    public BigInteger convert(Decimal128 source) {
        return source.bigDecimalValue().toBigInteger();
    }
}
