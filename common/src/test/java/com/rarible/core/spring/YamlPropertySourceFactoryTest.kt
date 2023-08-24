package com.rarible.core.spring

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.SpringBootConfiguration
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.PropertySource

@SpringBootTest
@SpringBootConfiguration
@EnableAutoConfiguration
@PropertySource(
    value = ["classpath:test.yml"],
    factory = YamlPropertySourceFactory::class
)
@EnableConfigurationProperties(YamlPropertySourceFactoryTest.TestProperties::class)
class YamlPropertySourceFactoryTest {
    @Autowired
    private lateinit var properties: TestProperties

    @Test
    fun readYmlConfig() {
        assertThat(properties.config).isEqualTo("ok")
    }

    @ConstructorBinding
    @ConfigurationProperties("test")
    data class TestProperties(
        val config: String
    )
}