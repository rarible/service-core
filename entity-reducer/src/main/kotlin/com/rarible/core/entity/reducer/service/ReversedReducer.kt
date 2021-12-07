package com.rarible.core.entity.reducer.service

interface ReversedReducer<Event : Comparable<Event>, E> : Reducer<Event, E>
