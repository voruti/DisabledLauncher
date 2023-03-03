package de.redno.disabledlauncher.model.exception

open class DisabledLauncherException(message: String?, cause: Throwable?) : Exception(message, cause) {
    constructor(message: String?) : this(message, null)
    constructor(cause: Throwable) : this(null, cause)
}
