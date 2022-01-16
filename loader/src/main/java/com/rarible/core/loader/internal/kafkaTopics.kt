package com.rarible.core.loader.internal

import com.rarible.core.loader.LoadType

fun getLoadTasksTopic(type: LoadType) = "loader-tasks-$type"

fun getLoadNotificationsTopic(type: LoadType) = "loader-notifications-$type"
