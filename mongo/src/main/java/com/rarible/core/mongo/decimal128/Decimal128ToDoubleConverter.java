package com.rarible.core.mongo.decimal128;

import org.bson.types.Decimal128;
import org.springframework.core.convert.converter.Converter;

public class Decimal128ToDoubleConverter implements Converter<Decimal128, Double> {
    @Override
    public Double convert(Decimal128 source) {
        return source.bigDecimalValue().doubleValue();
    }
}
