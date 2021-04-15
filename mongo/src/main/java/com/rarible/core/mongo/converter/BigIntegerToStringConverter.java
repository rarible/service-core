package com.rarible.core.mongo.converter;

import org.springframework.core.convert.converter.Converter;

import java.math.BigInteger;

public class BigIntegerToStringConverter implements Converter<BigInteger, String> {
    @Override
    public String convert(BigInteger source) {
        return source.toString();
    }
}
