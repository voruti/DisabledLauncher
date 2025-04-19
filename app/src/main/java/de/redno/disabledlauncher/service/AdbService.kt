package de.redno.disabledlauncher.service

import android.content.Context
import android.content.pm.PackageManager
import de.redno.disabledlauncher.R
import de.redno.disabledlauncher.model.exception.DisabledLauncherException
import de.redno.disabledlauncher.model.exception.NoShizukuPermissionException
import de.redno.disabledlauncher.model.exception.ShizukuException
import de.redno.disabledlauncher.model.exception.ShizukuUnavailableException
import de.redno.disabledlauncher.model.exception.ShizukuVersionNotSupportedException
import rikka.shizuku.Shizuku

object AdbService {
    @Throws(ShizukuException::class)
    fun checkShizukuPermission() {
        try {
            if (Shizuku.isPreV11()) {
                // Pre-v11 is unsupported
                throw ShizukuVersionNotSupportedException()
            } else if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
                // Granted
                return
            } else if (!Shizuku.shouldShowRequestPermissionRationale()) {
                // Users choose "Deny and don't ask again"
                throw NoShizukuPermissionException()
            } else {
                // Request the permission
                Shizuku.requestPermission(0)
                throw NoShizukuPermissionException()
            }
        } catch (e: IllegalStateException) {
            throw ShizukuUnavailableException()
        }
    }

    @Throws(DisabledLauncherException::class)
    fun executeAdbCommand(context: Context, command: String) {
        checkShizukuPermission()

        try {
            if (Shizuku.newProcess(arrayOf("sh", "-c", command), null, null).waitFor() != 0) {
                throw DisabledLauncherException(context.getString(R.string.process_failure))
            }
        } catch (e: IllegalStateException) {
            throw DisabledLauncherException(context.getString(R.string.process_failure))
        }
    }
}
