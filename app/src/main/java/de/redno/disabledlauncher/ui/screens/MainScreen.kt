package de.redno.disabledlauncher.ui.screens

import android.widget.Toast
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import de.redno.disabledlauncher.R
import de.redno.disabledlauncher.common.AndroidUtil
import de.redno.disabledlauncher.model.App
import de.redno.disabledlauncher.model.exception.DisabledLauncherException
import de.redno.disabledlauncher.service.AppService
import de.redno.disabledlauncher.ui.theme.DisabledLauncherTheme
import kotlinx.coroutines.launch

sealed class Screen(
    val route: String,
    val imageVector: ImageVector,
    @StringRes val iconDescriptionResourceId: Int,
    @StringRes val labelResourceId: Int
) {
    object DirectLauncher : Screen(
        "directlauncher",
        Icons.Default.Apps,
        R.string.direct_launcher_icon,
        R.string.direct_launcher_title
    )

    object LongTermLauncher : Screen(
        "longtermlauncher",
        Icons.AutoMirrored.Filled.DirectionsRun,
        R.string.long_term_launcher_icon,
        R.string.long_term_launcher_title
    )

    object DisableAppsOnce : Screen(
        "disableappsonce",
        Icons.Default.Block,
        R.string.disable_apps_once_icon,
        R.string.disable_apps_once_title
    )

    object EnableAppsOnce : Screen(
        "enableappsonce",
        Icons.Default.Check,
        R.string.enable_apps_once_icon,
        R.string.enable_apps_once_title
    )
}


@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun MainPreview() {
    val context = LocalContext.current

    DisabledLauncherTheme {
        MainScreen(onSettingsClick = {}, directLauncherAppList = listOf("de.test.1").map {
            AppService.getDetailsForPackage(context, it)
        }, longTermLauncherAppList = listOf("de.test.2").map {
            AppService.getDetailsForPackage(context, it)
        },
            modifier = Modifier.safeDrawingPadding()
        )
    }
}


@Composable
fun MainScreen(
    onSettingsClick: () -> Unit,
    directLauncherAppList: List<App>,
    longTermLauncherAppList: List<App>,
    modifier: Modifier = Modifier,
    drawerNavController: NavHostController = rememberNavController(),
    drawerStartDestination: String = Screen.DirectLauncher.route
) {
    val context = LocalContext.current

    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    fun toggleDrawer() {
        scope.launch {
            drawerState.apply {
                if (isClosed) open() else close()
                // TODO: close on back gesture
            }
        }
    }

    // close after leaving settings:
    // TODO: negative side effect: also closed on screen rotation
    run {
        scope.launch {
            drawerState.apply {
                if (isOpen) close()
            }
        }
    }

    ModalNavigationDrawer(
        modifier = modifier, drawerState = drawerState, drawerContent = {
            ModalDrawerSheet {
                Box(
                    modifier = Modifier
                        .height(64.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text(
                        text = stringResource(id = R.string.app_name),
                        modifier = Modifier.padding(horizontal = 16.dp),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                HorizontalDivider()

                val navBackStackEntry by drawerNavController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination

                listOf(
                    Screen.DirectLauncher,
                    Screen.LongTermLauncher,
                    Screen.DisableAppsOnce,
                    Screen.EnableAppsOnce
                ).forEach { screen ->
                    DrawerItem(
                        imageVector = screen.imageVector,
                        iconDescriptionResourceId = screen.iconDescriptionResourceId,
                        labelResourceId = screen.labelResourceId,
                        selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                        onClick = {
                            toggleDrawer()
                            drawerNavController.navigate(screen.route) {
                                // Pop up to the start destination of the graph to
                                // avoid building up a large stack of destinations
                                // on the back stack as users select items
                                popUpTo(drawerNavController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                // Avoid multiple copies of the same destination when
                                // reselecting the same item
                                launchSingleTop = true
                                // Restore state when reselecting a previously selected item
                                restoreState = true
                            }
                        })
                }

                HorizontalDivider()

                DrawerItem(
                    imageVector = Icons.Default.Settings,
                    iconDescriptionResourceId = R.string.settings,
                    labelResourceId = R.string.settings,
                    onClick = {
                        toggleDrawer()
                        onSettingsClick()
                    })
            }
        }) {
        Scaffold {
            NavHost( // TODO: combine with other nav controller in MainActivity?
                navController = drawerNavController,
                startDestination = drawerStartDestination,
                modifier = Modifier.consumeWindowInsets(it)
            ) {
                composable(Screen.DirectLauncher.route) {
                    DirectLauncherScreen(
                        onMenuClick = { toggleDrawer() }, appList = directLauncherAppList
                    )
                }

                composable(Screen.LongTermLauncher.route) {
                    LongTermLauncherScreen(
                        onMenuClick = { toggleDrawer() }, appList = longTermLauncherAppList
                    )
                }

                composable(Screen.DisableAppsOnce.route) {
                    val enabledApps =
                        AppService.getInstalledPackages(context) { it.applicationInfo!!.enabled }
                            .map { AppService.getDetailsForPackage(context, it) }
                    // TODO: don't getInstalledPackages, throw everything but the package name away, and then get the details again - instead: use the data that is already there

                    SelectMultipleAppsScreen(
                        onMenuClick = { toggleDrawer() },
                        title = stringResource(R.string.disable_apps_once_title),
                        selectableApps = enabledApps,
                        onConfirmSelection = {
                            try {
                                it.forEach {
                                    AppService.disableApp(context, it, true)
                                }
                            } catch (e: DisabledLauncherException) {
                                e.getLocalizedMessage(context)?.let {
                                    AndroidUtil.asyncToastMakeText(context, it, Toast.LENGTH_SHORT)
                                }
                            }
                        })
                }

                composable(Screen.EnableAppsOnce.route) {
                    val disabledApps =
                        AppService.getInstalledPackages(context) { !it.applicationInfo!!.enabled }
                            .map { AppService.getDetailsForPackage(context, it) }

                    SelectMultipleAppsScreen(
                        onMenuClick = { toggleDrawer() },
                        title = stringResource(R.string.enable_apps_once_title),
                        selectableApps = disabledApps,
                        onConfirmSelection = {
                            try {
                                it.forEach {
                                    AppService.enableApp(context, it, true)
                                }
                            } catch (e: DisabledLauncherException) {
                                e.getLocalizedMessage(context)?.let {
                                    AndroidUtil.asyncToastMakeText(context, it, Toast.LENGTH_SHORT)
                                }
                            }
                        })
                }
            }
        }
    }
}

@Composable
private fun DrawerItem(
    imageVector: ImageVector,
    @StringRes iconDescriptionResourceId: Int,
    @StringRes labelResourceId: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false
) {
    Box(
        modifier = modifier
            .height(48.dp)
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .fillMaxWidth()
            .clickable { onClick() }
            .then(
                if (selected) Modifier.background(
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(4.dp)
                )
                else Modifier
            )) {
        Row {
            Icon(
                imageVector = imageVector,
                contentDescription = stringResource(iconDescriptionResourceId),
                modifier = Modifier
                    .padding(8.dp)
                    .padding(end = 32.dp),
                tint = if (selected) MaterialTheme.colorScheme.primary else Color.Gray
            )
            Box(
                modifier = Modifier.fillMaxHeight(), contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    text = stringResource(labelResourceId),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = FontWeight.Medium,
                    color = if (selected) MaterialTheme.colorScheme.primary else Color.Unspecified
                )
            }
        }
    }
}
