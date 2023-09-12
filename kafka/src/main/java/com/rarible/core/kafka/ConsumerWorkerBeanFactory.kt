package com.rarible.core.kafka

import org.springframework.beans.factory.config.BeanFactoryPostProcessor
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory

class ConsumerWorkerBeanFactory<T>(
    private val consumerSettings: RaribleKafkaConsumerSettings,
    private val factorySettings: RaribleKafkaContainerFactorySettings<T>,

) : BeanFactoryPostProcessor {
    override fun postProcessBeanFactory(beanFactory: ConfigurableListableBeanFactory) {
    }
}
