package com.rarible.core.apm.annotation

import com.rarible.core.apm.CaptureSpan
import com.rarible.core.apm.CaptureTransaction
import org.springframework.aop.framework.ProxyFactory
import org.springframework.beans.factory.config.BeanPostProcessor
import java.lang.reflect.AnnotatedElement

class SpanAnnotationPostProcessor : BeanPostProcessor {
    private val spanClasses: MutableMap<String, Class<*>> = HashMap()

    override fun postProcessBeforeInitialization(bean: Any, beanName: String): Any {
        val type = bean.javaClass

        val hasTargetAnnotations: (AnnotatedElement) -> Boolean = {
            it.isAnnotationPresent(CaptureSpan::class.java) || it.isAnnotationPresent(CaptureTransaction::class.java)
        }
        if (hasTargetAnnotations(type)) {
            spanClasses[beanName] = type
        }
        if (type.methods.any(hasTargetAnnotations) || type.declaredMethods.any(hasTargetAnnotations)) {
            spanClasses[beanName] = type
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
