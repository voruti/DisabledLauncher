package de.redno.disabledlauncher

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import de.redno.disabledlauncher.model.ListType
import de.redno.disabledlauncher.service.AppService
import de.redno.disabledlauncher.service.Datasource
import de.redno.disabledlauncher.ui.screens.MainScreen
import de.redno.disabledlauncher.ui.screens.Screen
import de.redno.disabledlauncher.ui.screens.SettingsScreen
import de.redno.disabledlauncher.ui.theme.DisabledLauncherTheme

class MainActivity : ComponentActivity() { // TODO: faster startup somehow?
    companion object {
        const val ROUTE_MAIN = "main"
        const val ROUTE_SETTINGS = "settings"

        var lastObject: MainActivity? = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lastObject = this
        enableEdgeToEdge()
        setContent {
            DisabledLauncherTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    DisabledLauncherNavHost(
                        modifier = Modifier.padding(innerPadding),
                        mainDrawerStartDestination = intent.getStringExtra("mainDrawerStartDestination")
                            ?: Screen.DirectLauncher.route
                    )
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

                    getSharedPreferences(packageName, MODE_PRIVATE)
                        .edit()
                        .putString("launchableAppsFile", it.toString())
                        .apply()
                }
            }
        }
}


@Composable
fun DisabledLauncherNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    startDestination: String = MainActivity.ROUTE_MAIN,
    mainDrawerStartDestination: String = Screen.DirectLauncher.route
) {
    val context = LocalContext.current

    NavHost(
        modifier = modifier,
        navController = navController,
        startDestination = startDestination
    ) {
        composable(MainActivity.ROUTE_MAIN) {
            MainScreen(
                drawerStartDestination = mainDrawerStartDestination,
                onSettingsClick = { navController.navigate(MainActivity.ROUTE_SETTINGS) },
                directLauncherAppList = Datasource.loadAppList(context, ListType.DIRECT).map {
                    AppService.getDetailsForPackage(context, it, ListType.DIRECT)
                }.toMutableList().apply {
                    addAll(Datasource.loadAppList(context, ListType.LONG_TERM).map {
                        AppService.getDetailsForPackage(context, it, ListType.LONG_TERM)
                    })
                },
                longTermLauncherAppList = Datasource.loadAppList(context, ListType.LONG_TERM).map {
                    AppService.getDetailsForPackage(context, it, ListType.LONG_TERM)
                }
            )
        }

        composable(MainActivity.ROUTE_SETTINGS) {
            SettingsScreen(
                onBackNavigation = { navController.navigateUp() }
            )
        }
    }
}
