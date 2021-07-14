package com.rarible.core.reduce.model

interface ReduceEvent<Mark : Comparable<Mark>> {
    val mark: Mark
}