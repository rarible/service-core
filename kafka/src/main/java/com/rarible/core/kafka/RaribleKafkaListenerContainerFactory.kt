package com.rarible.core.kafka

import com.rarible.core.kafka.json.JsonDeserializer
import com.rarible.core.kafka.json.RARIBLE_KAFKA_CLASS_PARAM
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.common.serialization.StringDeserializer
import org.slf4j.LoggerFactory
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.core.DefaultKafkaConsumerFactory
import org.springframework.kafka.listener.DefaultErrorHandler
import org.springframework.kafka.listener.adapter.RecordFilterStrategy
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer
import org.springframework.kafka.support.serializer.FailedDeserializationInfo
import java.util.function.Function

class RaribleKafkaListenerContainerFactory<T>(
    private val settings: RaribleKafkaContainerFactorySettings<T>,
) : ConcurrentKafkaListenerContainerFactory<String, T>() {

    init {
        consumerFactory = DefaultKafkaConsumerFactory(consumerConfigs(settings.valueClass) + settings.customSettings)
        isBatchListener = settings.batchSize > 1
        setConcurrency(settings.concurrency)
        if (isBatchListener) {
            setCommonErrorHandler(DefaultErrorHandler(if (settings.shouldSkipEventsOnError) null else NoRecover()))
        }
        setRecordFilterStrategy(NullFilteringStrategy<T>())
    }

    private fun consumerConfigs(valueClass: Class<*>): Map<String, Any> {
        return mapOf(
            ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG to ErrorHandlingDeserializer::class.java,
            ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG to ErrorHandlingDeserializer::class.java,
            ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG to settings.hosts,
            ConsumerConfig.AUTO_OFFSET_RESET_CONFIG to settings.offsetResetStrategy.name.lowercase(),
            ErrorHandlingDeserializer.KEY_DESERIALIZER_CLASS to StringDeserializer::class.java,
            ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS to (settings.deserializer
                ?: if (valueClass == String::class.java) {
                    StringDeserializer::class.java
                } else {
                    JsonDeserializer::class.java
                }),
            ErrorHandlingDeserializer.KEY_FUNCTION to LoggingDeserializationFailureFunction::class.java,
            ErrorHandlingDeserializer.VALUE_FUNCTION to LoggingDeserializationFailureFunction::class.java,
            ConsumerConfig.MAX_POLL_RECORDS_CONFIG to settings.batchSize,
            RARIBLE_KAFKA_CLASS_PARAM to valueClass
        )
    }
}

class LoggingDeserializationFailureFunction : Function<FailedDeserializationInfo, Any?> {
    override fun apply(t: FailedDeserializationInfo): Any? {
        logger.error("Failed to deserialize ${String(t.data)} from topic: ${t.topic}", t.exception)
        return null
    }

    companion object {
        private val logger = LoggerFactory.getLogger(LoggingDeserializationFailureFunction::class.java)
    }
}

class NullFilteringStrategy<V> : RecordFilterStrategy<String, V> {
    override fun filter(consumerRecord: ConsumerRecord<String, V?>): Boolean = consumerRecord.value() == null
}
