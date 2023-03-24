package de.redno.disabledlauncher.service

import android.content.pm.PackageManager
import android.content.res.Resources
import androidx.compose.material.*
import androidx.compose.runtime.*
import de.redno.disabledlauncher.R
import de.redno.disabledlauncher.model.*
import de.redno.disabledlauncher.model.exception.*
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
    fun executeAdbCommand(command: String) {
        checkShizukuPermission()

        if (Shizuku.newProcess(arrayOf("sh", "-c", command), null, null).waitFor() != 0) {
            throw DisabledLauncherException(Resources.getSystem().getString(R.string.process_failure))
        }
    }
}
