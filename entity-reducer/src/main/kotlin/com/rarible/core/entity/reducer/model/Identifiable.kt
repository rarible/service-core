package com.rarible.core.entity.reducer.model

interface Identifiable<Id> {
    val id: Id
    /*
     * Version for optimistic lock
     */
    val version: Long?
}
