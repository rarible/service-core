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
            DockerImageName.parse("docker.elastic.co/elasticsearch/elasticsearch:8.7.0")

        @JvmStatic
        private val elasticsearch: ElasticsearchContainer by lazy {
            ElasticsearchContainer(ELASTIC_SEARCH__IMAGE).apply {
                withReuse(true)
                withEnv("xpack.security.enabled", "false")
                withEnv("xpack.security.http.ssl.enabled", "false")
                withEnv("xpack.security.transport.ssl.enabled", "false")
                withEnv("CLI_JAVA_OPTS", "-Xms128m -Xmx512m")
            }
        }

        init {
            elasticsearch.start()
        }
    }
}
