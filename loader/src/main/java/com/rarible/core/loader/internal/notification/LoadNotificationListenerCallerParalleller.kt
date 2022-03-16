package com.rarible.core.loader.internal.notification

import com.rarible.core.loader.LoadNotification
import com.rarible.core.loader.internal.common.LoadParalleller

class LoadNotificationListenerCallerParalleller(
    numberOfThreads: Int,
    loadNotificationListenersCaller: LoadNotificationListenersCaller
) : LoadParalleller<LoadNotification>(
    numberOfThreads = numberOfThreads,
    threadPrefix = "load-notification-listener-caller",
    runner = { notification -> loadNotificationListenersCaller.notifyListeners(notification) }
)
