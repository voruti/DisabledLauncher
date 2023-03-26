package de.redno.disabledlauncher

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import de.redno.disabledlauncher.common.AndroidUtil
import de.redno.disabledlauncher.model.exception.DisabledLauncherException
import de.redno.disabledlauncher.service.AppService
import de.redno.disabledlauncher.service.Datasource
import de.redno.disabledlauncher.ui.screens.DirectLauncherScreen
import de.redno.disabledlauncher.ui.screens.SettingsScreen
import de.redno.disabledlauncher.ui.theme.DisabledLauncherTheme

class MainActivity : ComponentActivity() { // TODO: faster startup somehow?
    companion object {
        const val ROUTE_DIRECT_LAUNCHER = "directlauncher"
        const val ROUTE_SETTINGS = "settings"

        var lastObject: MainActivity? = null

        fun exit() {
            lastObject?.finishAndRemoveTask()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lastObject = this
        setContent {
            DisabledLauncherTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colors.background
                ) {
                    DisabledLauncherNavHost()
                }
            }
        }
    }

    val pickLaunchableAppsFileResultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == RESULT_OK) { // TODO: move into function like SelectAppsActivity.registerCallback
                it.data?.data?.let {
                    contentResolver.takePersistableUriPermission(
                        it,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION or
                                Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    )

                    getSharedPreferences(packageName, Context.MODE_PRIVATE)
                        .edit()
                        .putString("launchableAppsFile", it.toString())
                        .apply()
                }
            }
        }

    val addAppsResultLauncher = SelectAppsActivity.registerCallback(this) {
        if (!Datasource.addPackages(this, it)) {
            AndroidUtil.asyncToastMakeText(
                this,
                this.getString(R.string.failed_adding_apps),
                Toast.LENGTH_SHORT
            )
        }
    }

    val disableAppsOnceResultLauncher = SelectAppsActivity.registerCallback(this,
        { it.map { AppService.getDetailsForPackage(this, it) } }) {
        try {
            it.forEach {
                AppService.disableApp(this, it, true)
            }
        } catch (e: DisabledLauncherException) {
            e.getLocalizedMessage(this)?.let {
                AndroidUtil.asyncToastMakeText(this, it, Toast.LENGTH_SHORT)
            }
        }
    }

    val enableAppsOnceResultLauncher = SelectAppsActivity.registerCallback(this,
        { it.map { AppService.getDetailsForPackage(this, it) } }) {
        try {
            it.forEach {
                AppService.enableApp(this, it, true)
            }
        } catch (e: DisabledLauncherException) {
            e.getLocalizedMessage(this)?.let {
                AndroidUtil.asyncToastMakeText(this, it, Toast.LENGTH_SHORT)
            }
        }
    }
}


@Composable
fun DisabledLauncherNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    startDestination: String = MainActivity.ROUTE_DIRECT_LAUNCHER
) {
    val context = LocalContext.current

    NavHost(
        modifier = modifier,
        navController = navController,
        startDestination = startDestination
    ) {
        composable(MainActivity.ROUTE_DIRECT_LAUNCHER) {
            DirectLauncherScreen(
                { navController.navigate(MainActivity.ROUTE_SETTINGS) },
                Datasource.loadAppList(context)
            )
        }
        composable(MainActivity.ROUTE_SETTINGS) {
            SettingsScreen()
        }
    }
}
