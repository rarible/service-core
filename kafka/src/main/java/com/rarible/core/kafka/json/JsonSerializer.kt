package com.rarible.core.kafka.json

import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.kafka.common.header.Headers
import org.apache.kafka.common.serialization.Serializer

const val RARIBLE_KAFKA_CLASS_HEADER = "RARIBLE_KAFKA_CLASS_HEADER"
const val RARIBLE_KAFKA_CONTAINER_CLASS_HEADER = "RARIBLE_KAFKA_CONTAINER_CLASS_HEADER"
const val RARIBLE_KAFKA_KEY_CLASS_HEADER = "RARIBLE_KAFKA_KEY_CLASS_HEADER"
const val RARIBLE_KAFKA_CLASS_PARAM = "RARIBLE_KAFKA_CLASS_PARAM"

open class JsonSerializer : Serializer<Any> {

    private lateinit var objectMapper: ObjectMapper
    private var valueClass: Class<*>? = null

    override fun configure(configs: MutableMap<String, *>?, isKey: Boolean) {
        valueClass = configs?.get(RARIBLE_KAFKA_CLASS_PARAM) as Class<*>?
        objectMapper = createMapper()
    }

    open fun createMapper(): ObjectMapper {
        return KafkaObjectMapperFactory.getMapper()
    }

    override fun serialize(topic: String?, data: Any): ByteArray {
        return serialize(topic, null, data)
    }

    override fun serialize(topic: String?, headers: Headers?, data: Any): ByteArray {
        if (headers != null) {
            if (valueClass != null) {
                headers.add(RARIBLE_KAFKA_CLASS_HEADER, valueClass!!.name.toByteArray())
            } else {
                val type = objectMapper.constructType(data::class.java)
                headers.add(RARIBLE_KAFKA_CLASS_HEADER, type.rawClass.name.toByteArray())
                if (type.isContainerType() && !type.isArrayType()) {
                    headers.add(RARIBLE_KAFKA_CONTAINER_CLASS_HEADER, type.contentType.rawClass.name.toByteArray())
                }
                if (type.getKeyType() != null) {
                    headers.add(RARIBLE_KAFKA_KEY_CLASS_HEADER, type.keyType.rawClass.name.toByteArray())
                }
            }
        }
        return objectMapper.writeValueAsBytes(data)
    }
}