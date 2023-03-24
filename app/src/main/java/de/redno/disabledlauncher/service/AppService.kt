package de.redno.disabledlauncher.service

import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.core.graphics.drawable.toBitmap
import de.redno.disabledlauncher.R
import de.redno.disabledlauncher.common.AndroidUtil
import de.redno.disabledlauncher.model.App
import de.redno.disabledlauncher.model.exception.DisabledLauncherException
import de.redno.disabledlauncher.model.exception.RedirectedToGooglePlayException

object AppService {
    fun getInstalledPackages(
        context: Context,
        packageInfoFilter: (packageInfo: PackageInfo) -> Boolean = { true }
    ): List<String> {
        val packageManager = context.packageManager

        return packageManager.getInstalledPackages(PackageManager.GET_META_DATA)
            .filter(packageInfoFilter)
            .map { it.packageName }
    }

    fun getDetailsForPackage(context: Context, packageName: String): App {
        val packageManager = context.packageManager

        try {
            packageManager.getPackageInfo(packageName, 0)?.let {
                return App(
                    it.applicationInfo.loadLabel(packageManager).toString(),
                    it.packageName,
                    it.applicationInfo.enabled,
                    true,
                    it.applicationInfo.loadIcon(packageManager).toBitmap()
                )
            }
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
        }

        return App(
            name = context.getString(R.string.app_not_found),
            packageName = packageName,
            icon = context.getDrawable(R.drawable.ic_launcher_background)!!
                .toBitmap(), // TODO: add proper app icons (+ for static shortcut, etc.)
            isEnabled = false,
            isInstalled = false
        )
    }

    @Throws(DisabledLauncherException::class)
    fun enableApp(context: Context, packageName: String) {
        try {
            AdbService.executeAdbCommand("pm enable $packageName")
        } catch (e: DisabledLauncherException) {
            val sharedPreferences = context.getSharedPreferences(context.packageName, Context.MODE_PRIVATE)
            val fallbackToGooglePlay = sharedPreferences.getBoolean("fallbackToGooglePlay", false)

            if (fallbackToGooglePlay) {
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    data = Uri.parse("https://play.google.com/store/apps/details?id=$packageName")
                }
                context.startActivity(intent)
                throw RedirectedToGooglePlayException(e)
            }

            throw e
        }
    }

    @Throws(DisabledLauncherException::class)
    fun disableAllApps(context: Context) {
        val appsToDisable = Datasource.loadAppList(context)
            .map { getDetailsForPackage(context, it) }
            .filter { it.isEnabled }

        if (appsToDisable.isEmpty()) {
            AndroidUtil.asyncToastMakeText(context, context.getString(R.string.nothing_to_disable), Toast.LENGTH_SHORT)
            return
        }

        appsToDisable.forEach {
            disableApp(context, it)
        }
    }

    @Throws(DisabledLauncherException::class)
    fun disableApp(context: Context, app: App) { // TODO: extract into service
        AdbService.executeAdbCommand("pm disable-user --user 0 ${app.packageName}")

        AndroidUtil.asyncToastMakeText(
            context,
            String.format(context.getString(R.string.disabled_app), app.name),
            Toast.LENGTH_SHORT
        )
    }

    @Throws(DisabledLauncherException::class)
    fun openAppLogic(context: Context, app: App) {
        if (!app.isEnabled) {
            enableApp(context, app.packageName)
        }

        startApp(context, app.packageName)
    }

    @Throws(DisabledLauncherException::class)
    fun startApp(context: Context, packageName: String) {
        try {
            val launchIntent = context.packageManager.getLaunchIntentForPackage(packageName)
                ?.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
            context.startActivity(launchIntent)
        } catch (e: Exception) {
            throw DisabledLauncherException(context.getString(R.string.cant_open_app))
        }
    }
}
