package com.rarible.core.task

import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.runBlocking
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.index.Index
import org.springframework.stereotype.Component

@Component
class TaskIndexes(
    private val mongo: ReactiveMongoTemplate
) {

    @EventListener(ApplicationReadyEvent::class)
    fun createIndexes() = runBlocking {
        ALL_INDEXES.forEach { index ->
            mongo.indexOps("task").ensureIndex(index).awaitFirst()
        }
    }

    companion object {

        private val RUNNING_AND_STATUS: Index = Index()
            .on(Task::running.name, Sort.Direction.ASC)
            .on(Task::lastStatus.name, Sort.Direction.ASC)
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
