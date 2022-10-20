package com.rarible.core.entity.reducer.service;

public enum LogStatus {

    CONFIRMED,

    REVERTED,

    @Deprecated PENDING,

    @Deprecated DROPPED,

    @Deprecated INACTIVE;
}
