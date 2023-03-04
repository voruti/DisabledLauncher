package de.redno.disabledlauncher.model.exception

import android.content.Context
import de.redno.disabledlauncher.R

open class DisabledLauncherException(message: String?, cause: Throwable?) : Exception(message, cause) {
    constructor(message: String?) : this(message, null)
    constructor(cause: Throwable) : this(null, cause)

    fun getLocalizedMessage(context: Context): String? {
        fun getMessage(e: Throwable?): String? {
            return when (e) {
                is NoShizukuPermissionException -> context.getString(R.string.shizuku_denied)
                is ShizukuUnavailableException -> context.getString(R.string.shizuku_unavailable)
                is ShizukuVersionNotSupportedException -> context.getString(R.string.shizuku_unsupported_version)

                else -> null
            }
        }

        return getMessage(this) ?: getMessage(cause) ?: message
    }
}
