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
annotation class ElasticsearchTest

class ElasticSearchTestExtension : BeforeAllCallback {

    override fun beforeAll(context: ExtensionContext) {
        System.setProperty(
            "elasticsearch.cluster-nodes",
            esContainer.getClusterNodes()
        )

        System.setProperty(
            "elasticsearch.api-nodes",
            esContainer.getApiNodes()
        )
    }

    companion object {
        val esContainer = ElasticsearchTestContainer()
    }
}