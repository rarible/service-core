package com.rarible.core.test.containers

import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.elasticsearch.ElasticsearchContainer
import org.testcontainers.utility.DockerImageName

class ElasticsearchTestContainer {

    fun elasticUrl(): String {
        return elasticsearch.httpHostAddress
    }

    companion object {
        val ELASTIC_SEARCH__IMAGE: DockerImageName =
            DockerImageName.parse("docker.elastic.co/elasticsearch/elasticsearch:7.9.3")

        @JvmStatic
        private val elasticsearch: ElasticsearchContainer by lazy {
            ElasticsearchContainer(ELASTIC_SEARCH__IMAGE).apply {
                withEnv("discovery.type", "single-node")
                waitingFor(Wait.defaultWaitStrategy())
                withReuse(true)
            }
        }

        init {
            elasticsearch.start()
        }
    }
}