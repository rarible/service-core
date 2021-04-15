package com.rarible.core.mongo.converter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.core.convert.converter.Converter;

import java.util.List;
import java.util.Optional;

import static java.util.Arrays.asList;

public class ObjectNodeMongoConverter implements CustomMongoConverter {
    private final ObjectMapper mapper;

    public ObjectNodeMongoConverter(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    @SuppressWarnings("rawtypes")
    @Override
    public List<Converter> getConverters() {
        return asList(
            new DocumentToObjectNodeConverter(mapper),
            new ObjectNodeToStringConverter(mapper),
            new StringToObjectNodeConverter(mapper)
        );
    }

    @Override
    public boolean isSimpleType(Class<?> aClass) {
        return ObjectNode.class.equals(aClass);
    }

    @Override
    public Optional<Class<?>> getCustomWriteTarget(Class<?> sourceType) {
        if (sourceType == ObjectNode.class) {
            return Optional.of(String.class);
        }
        return Optional.empty();
    }
}
