package com.rarible.core.task

import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactor.awaitSingleOrNull
import kotlinx.coroutines.runBlocking
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.index.Index
import org.springframework.data.mongodb.core.index.ReactiveIndexOperations
import org.springframework.stereotype.Component

@Component
class TaskIndexes(
    private val mongo: ReactiveMongoTemplate
) {

    @EventListener(ApplicationReadyEvent::class)
    fun createIndexes() = runBlocking {
        val indexOps = mongo.indexOps("task")
        ALL_INDEXES.forEach { index -> indexOps.ensureIndex(index).awaitFirst() }
        indexOps.dropIndexIfExists("running_1_lastStatus_1__id_1")
    }

    companion object {

        private val RUNNING_AND_STATUS: Index = Index()
            .on(Task::running.name, Sort.Direction.ASC)
            .on(Task::lastStatus.name, Sort.Direction.ASC)
            .on(Task::priority.name, Sort.Direction.DESC)
            .on("_id", Sort.Direction.ASC)
            .background()

        private val TYPE_AND_PARAM_UINQUE: Index = Index()
            .on(Task::type.name, Sort.Direction.ASC)
            .on(Task::param.name, Sort.Direction.ASC)
            .unique()
            .background()

        private val ALL_INDEXES = listOf(
            RUNNING_AND_STATUS,
            TYPE_AND_PARAM_UINQUE,
        )
    }
}

private suspend fun ReactiveIndexOperations.dropIndexIfExists(name: String) {
    indexInfo.filter { it.name == name }.awaitFirstOrNull()?.let {
        dropIndex(it.name).awaitSingleOrNull()
    }
}
