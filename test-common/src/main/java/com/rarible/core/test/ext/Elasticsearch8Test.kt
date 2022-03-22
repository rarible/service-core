package com.rarible.core.test.ext

import com.rarible.core.test.containers.ElasticsearchTestContainer
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.ExtensionContext
import java.lang.annotation.Inherited


@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@Inherited
@ExtendWith(ElasticSearchTestExtension::class)
annotation class Elasticsearch8Test

class ElasticSearchTestExtension : BeforeAllCallback {

    override fun beforeAll(context: ExtensionContext) {
        System.setProperty("elasticsearch.host", esContainer.elasticUrl())
    }

    companion object {
        val esContainer = ElasticsearchTestContainer()
    }
}