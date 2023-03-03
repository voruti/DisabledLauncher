package de.redno.disabledlauncher.model.exception

class RedirectedToGooglePlayException(cause: Throwable) : DisabledLauncherException(cause.message, cause)
