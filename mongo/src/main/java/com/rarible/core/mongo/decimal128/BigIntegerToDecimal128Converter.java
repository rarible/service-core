package com.rarible.core.mongo.decimal128;

import org.bson.types.Decimal128;
import org.springframework.core.convert.converter.Converter;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;

public class BigIntegerToDecimal128Converter implements Converter<BigInteger, Decimal128> {
    @Override
    public Decimal128 convert(BigInteger source) {
        return new Decimal128(new BigDecimal(source).round(MathContext.DECIMAL128));
    }
}
