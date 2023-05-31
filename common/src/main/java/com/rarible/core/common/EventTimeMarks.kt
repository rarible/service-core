package com.rarible.core.common

import java.time.Instant

data class EventTimeMarks(
    val source: String,
    val marks: List<EventTimeMark> = emptyList()
) {

    fun add(name: String, date: Instant? = null): EventTimeMarks {
        val marks = this.marks.toMutableList()
        marks.add(EventTimeMark(name, date ?: Instant.now()))
        return this.copy(marks = marks)
    }

    fun addStage(markPointName: String, postfix: String?, date: Instant? = null): EventTimeMarks {
        val fullMarkPointName = if (postfix.isNullOrBlank()) {
            markPointName
        } else {
            "${markPointName}_$postfix"
        }
        return add(fullMarkPointName, date)
    }

    fun addIn(stage: String, postfix: String?, date: Instant? = null) = addStage("$stage-in", postfix, date)
    fun addOut(stage: String, postfix: String?, date: Instant? = null) = addStage("$stage-out", postfix, date)

}

data class EventTimeMark(
    val name: String,
    val date: Instant
)
