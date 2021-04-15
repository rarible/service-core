package com.rarible.core.mongo.jackson;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import org.apache.commons.lang3.StringUtils;
import org.bson.types.ObjectId;
import org.springframework.boot.jackson.JsonComponent;

import java.io.IOException;

@JsonComponent
public class ObjectIdCombinedSerializer {
    public static class Deserializer extends StdDeserializer<ObjectId> {
        public Deserializer() {
            super(ObjectId.class);
        }

        @Override
        public ObjectId deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            String text = p.getValueAsString();
            if (StringUtils.isNotBlank(text)) {
                return new ObjectId(text);
            } else {
                return null;
            }
        }
    }

    public static class Serializer extends StdSerializer<ObjectId> {
        public Serializer() {
            super(ObjectId.class);
        }

        @Override
        public void serialize(ObjectId value, JsonGenerator gen, SerializerProvider provider) throws IOException {
            if (value != null) {
                gen.writeString(value.toHexString());
            } else {
                gen.writeNull();
            }
        }
    }
}
