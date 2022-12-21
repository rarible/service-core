package com.rarible.core.test.containers

import org.testcontainers.elasticsearch.ElasticsearchContainer
import org.testcontainers.utility.DockerImageName

class ElasticsearchTestContainer {

    fun elasticUrl(): String {
        return elasticsearch.httpHostAddress
    }

    fun getClusterNodes(): String {
        return "${elasticsearch.containerIpAddress}:${elasticsearch.getMappedPort(9300)}"
    }

    fun getApiNodes(): String {
        return elasticsearch.httpHostAddress
    }

    companion object {
        val ELASTIC_SEARCH__IMAGE: DockerImageName =
            DockerImageName.parse("elasticsearch:7.17.8")
                .asCompatibleSubstituteFor("docker.elastic.co/elasticsearch/elasticsearch")

        @JvmStatic
        private val elasticsearch: ElasticsearchContainer by lazy {
            ElasticsearchContainer(ELASTIC_SEARCH__IMAGE).apply {
                withReuse(true)
            }
        }

        init {
            elasticsearch.start()
        }
    }
}