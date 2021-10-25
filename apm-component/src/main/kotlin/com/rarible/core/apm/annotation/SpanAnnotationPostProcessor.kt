package com.rarible.core.apm.annotation

import com.rarible.core.apm.Spanable
import org.springframework.aop.framework.ProxyFactory
import org.springframework.beans.factory.config.BeanPostProcessor

class SpanAnnotationPostProcessor : BeanPostProcessor {
    private val spanClasses: MutableMap<String, Class<*>> = HashMap()

    override fun postProcessBeforeInitialization(bean: Any, beanName: String): Any {
        val type = bean.javaClass

        if (type.isAnnotationPresent(Spanable::class.java)) {
            spanClasses[beanName] = bean.javaClass
        }
        return bean
    }

    override fun postProcessAfterInitialization(bean: Any, beanName: String): Any {
        val type = spanClasses[beanName] ?: return bean

        val factory = ProxyFactory(bean)
        factory.addAdvice(MonoSpanInvocationHandler(type))
        return factory.proxy
    }
}
