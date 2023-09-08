package com.rarible.core.kafka.json

import com.fasterxml.jackson.databind.JavaType
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.type.TypeFactory
import org.apache.kafka.common.header.Headers
import org.apache.kafka.common.serialization.Deserializer

open class JsonDeserializer : Deserializer<Any> {

    private lateinit var objectMapper: ObjectMapper
    private var valueClass: Class<*>? = null

    override fun configure(configs: MutableMap<String, *>?, isKey: Boolean) {
        valueClass = configs?.get(RARIBLE_KAFKA_CLASS_PARAM) as Class<*>?
        objectMapper = createMapper()
    }

    open fun createMapper(): ObjectMapper {
        return KafkaObjectMapperFactory.getMapper()
    }

    override fun deserialize(topic: String?, data: ByteArray?): Any {
        return deserialize(topic, null, data)
    }

    override fun deserialize(topic: String?, headers: Headers?, data: ByteArray?): Any {
        if (valueClass != null) {
            return objectMapper.readValue(data, valueClass)
        } else {
            val type = getType(headers)
            return objectMapper.readerFor(type).readValue(data)
        }
    }

    private fun getType(headers: Headers?): JavaType {
        val typeIdHeader: String = retrieveHeaderAsString(headers!!, RARIBLE_KAFKA_CLASS_HEADER)

        val classType = TypeFactory.defaultInstance().constructType(Class.forName(typeIdHeader))
        if (!classType.isContainerType() || classType.isArrayType()) {
            return classType
        }
        val contentClassType = TypeFactory.defaultInstance()
            .constructType(Class.forName(retrieveHeaderAsString(headers, RARIBLE_KAFKA_CONTAINER_CLASS_HEADER)))
        if (classType.getKeyType() == null) {
            return TypeFactory.defaultInstance()
                .constructCollectionLikeType(classType.getRawClass(), contentClassType)
        }

        val keyClassType = TypeFactory.defaultInstance()
            .constructType(Class.forName(retrieveHeaderAsString(headers, RARIBLE_KAFKA_KEY_CLASS_HEADER)))
        return TypeFactory.defaultInstance()
            .constructMapLikeType(classType.getRawClass(), keyClassType, contentClassType)
    }

    protected fun retrieveHeaderAsString(headers: Headers, headerName: String): String {
        val headerValues = headers.headers(headerName).iterator()
        if (headerValues.hasNext()) {
            val headerValue = headerValues.next()
            return String(headerValue.value())
        }
        throw IllegalStateException("Class header not found")
    }
}
