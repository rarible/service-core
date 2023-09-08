package com.rarible.core.kafka

@Deprecated("Use KafkaConsumerFactory")
sealed class KafkaSendResult {
    abstract val id: String

    val isSuccess: Boolean get() = this is Success

    val isFailure: Boolean get() = !isSuccess

    data class Success(override val id: String) : KafkaSendResult()

    data class Fail(override val id: String, val exception: Exception) : KafkaSendResult()

    fun ensureSuccess(): Success = when (this) {
        is Success -> this
        is Fail -> throw exception
    }
}
