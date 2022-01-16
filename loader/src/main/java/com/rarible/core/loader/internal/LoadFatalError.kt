package com.rarible.core.loader.internal

class LoadFatalError : Error {
    constructor(message: String) : super(message)
    constructor(message: String, cause: Throwable) : super(message, cause)
}
