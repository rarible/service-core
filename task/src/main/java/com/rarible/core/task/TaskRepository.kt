package com.rarible.core.task

import org.springframework.data.repository.reactive.ReactiveCrudRepository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

/**
 * NOT A PUBLIC API.
 *
 * Internal database repository for [Task]s.
 */
interface TaskRepository : ReactiveCrudRepository<Task, String> {
    fun findByTypeAndParam(type: String, param: String): Mono<Task>

    fun findByRunning(running: Boolean): Flux<Task>

    fun findByRunningAndLastStatusOrderByIdAsc(running: Boolean, lastStatus: TaskStatus): Flux<Task>

    fun findByTypeAndParamRegex(type: String, paramRegex: String): Flux<Task>
}
