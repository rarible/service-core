package com.rarible.core.test.ext

import com.rarible.core.test.containers.KafkaTestContainer
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.ExtensionContext
import java.lang.annotation.Inherited

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@Inherited
@ExtendWith(KafkaTestExtension::class)
annotation class KafkaTest

class KafkaTestExtension : BeforeAllCallback {

    override fun beforeAll(context: ExtensionContext) {
        System.setProperty(
            "kafka.hosts", kafkaContainer.kafkaBoostrapServers()
        )
    }

    companion object {
        val kafkaContainer = KafkaTestContainer()
    }
}