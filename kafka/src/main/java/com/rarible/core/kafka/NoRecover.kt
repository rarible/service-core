package com.rarible.core.kafka

import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.kafka.listener.ConsumerRecordRecoverer

class NoRecover : ConsumerRecordRecoverer {
    override fun accept(record: ConsumerRecord<*, *>?, e: Exception?) {
        throw RuntimeException("No recovery supported for record: $record", e)
    }
}
