package com.rarible.core.mongo.converter;

import org.apache.commons.lang3.StringUtils;
import org.springframework.core.convert.converter.Converter;

import java.math.BigInteger;

public class StringToBigIntegerConverter implements Converter<String, BigInteger> {
    @Override
    public BigInteger convert(String source) {
        return StringUtils.isNotBlank(source) ? new BigInteger(source) : null;
    }
}
