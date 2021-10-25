package com.rarible.core.apm.annotation

import com.rarible.core.apm.CaptureSpan
import com.rarible.core.apm.CaptureTransaction
import org.springframework.beans.factory.config.BeanPostProcessor
import java.lang.reflect.Proxy

class SpanAnnotationPostProcessor : BeanPostProcessor {
    private val spanClasses: MutableMap<String, Class<*>> = HashMap()

    override fun postProcessBeforeInitialization(bean: Any, beanName: String): Any {
        val type = bean.javaClass

        if (type.isAnnotationPresent(CaptureSpan::class.java)) {
            spanClasses[beanName] = bean.javaClass
        } else if (type.isAnnotationPresent(CaptureTransaction::class.java)) {
            spanClasses[beanName] = bean.javaClass
        }
        return bean
    }

    override fun postProcessAfterInitialization(bean: Any, beanName: String): Any? {
        val type = spanClasses[beanName] ?: return bean
        return Proxy.newProxyInstance(
            type.classLoader,
            type.interfaces,
            MonoSpanInvocationHandler(type, bean)
        )
    }
}
