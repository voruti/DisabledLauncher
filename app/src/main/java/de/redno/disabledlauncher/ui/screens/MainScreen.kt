package de.redno.disabledlauncher.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.*
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
import de.redno.disabledlauncher.R
import de.redno.disabledlauncher.common.AndroidUtil
import de.redno.disabledlauncher.model.exception.DisabledLauncherException
import de.redno.disabledlauncher.service.AppService
import de.redno.disabledlauncher.ui.theme.DisabledLauncherTheme
import kotlinx.coroutines.launch

private const val SCREEN_DIRECT_LAUNCHER = 0 // TODO: use nav controller instead
private const val SCREEN_DISABLE_APPS_ONCE = 1
private const val SCREEN_ENABLE_APPS_ONCE = 2


@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun MainPreview() {
    DisabledLauncherTheme {
        MainScreen(
            onSettingsClick = {},
            directLauncherPackageNameList = listOf("de.test.1")
        )
    }
}


@Composable
fun MainScreen(
    onSettingsClick: () -> Unit,
    directLauncherPackageNameList: List<String>,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val scaffoldState = rememberScaffoldState()
    val scope = rememberCoroutineScope()

    fun toggleDrawer() {
        scope.launch {
            scaffoldState.drawerState.apply {
                if (isClosed) open() else close()
                // TODO: close on back gesture
            }
        }
    }

    // close after leaving settings:
    run {
        scope.launch {
            scaffoldState.drawerState.apply {
                if (isOpen) close()
            }
        }
    }

    var currentScreen by remember { mutableStateOf(SCREEN_DIRECT_LAUNCHER) }

    Scaffold(
        modifier = modifier,
        scaffoldState = scaffoldState,
        drawerContent = {
            Box(
                modifier = Modifier.height(64.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    text = stringResource(id = R.string.app_name),
                    modifier = Modifier.padding(horizontal = 16.dp),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Divider()

            DrawerItem(
                imageVector = Icons.Default.Apps,
                contentDescription = stringResource(R.string.direct_launcher_icon),
                text = stringResource(R.string.direct_launcher_title),
                onClick = {
                    currentScreen = SCREEN_DIRECT_LAUNCHER
                    toggleDrawer()
                },
                selected = currentScreen == SCREEN_DIRECT_LAUNCHER
            )
            DrawerItem(
                imageVector = Icons.Default.Block,
                contentDescription = stringResource(R.string.disable_apps_once_icon),
                text = stringResource(R.string.disable_apps_once_title),
                onClick = {
                    currentScreen = SCREEN_DISABLE_APPS_ONCE
                    toggleDrawer()
                },
                selected = currentScreen == SCREEN_DISABLE_APPS_ONCE
            )
            DrawerItem(
                imageVector = Icons.Default.Check,
                contentDescription = stringResource(R.string.enable_apps_once_icon),
                text = stringResource(R.string.enable_apps_once_title),
                onClick = {
                    currentScreen = SCREEN_ENABLE_APPS_ONCE
                    toggleDrawer()
                },
                selected = currentScreen == SCREEN_ENABLE_APPS_ONCE
            )

            Divider()

            DrawerItem(
                imageVector = Icons.Default.Settings,
                contentDescription = stringResource(R.string.settings),
                text = stringResource(R.string.settings),
                onClick = {
                    toggleDrawer()
                    onSettingsClick()
                },
                selected = false
            )
        }
    ) {
        when (currentScreen) {
            SCREEN_DIRECT_LAUNCHER ->
                DirectLauncherScreen(
                    onMenuClick = { toggleDrawer() },
                    packageNameList = directLauncherPackageNameList,
                    modifier = Modifier.padding(it)
                )

            SCREEN_DISABLE_APPS_ONCE -> {
                val enabledApps = AppService.getInstalledPackages(context) { it.applicationInfo.enabled }
                    .map { AppService.getDetailsForPackage(context, it) }

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
                    },
                    modifier = Modifier.padding(it)
                )
            }

            SCREEN_ENABLE_APPS_ONCE -> {
                val disabledApps = AppService.getInstalledPackages(context) { !it.applicationInfo.enabled }
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
                    },
                    modifier = Modifier.padding(it)
                )
            }
        }
    }
}

@Composable
private fun DrawerItem(
    imageVector: ImageVector,
    contentDescription: String,
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false
) {
    Box(
        modifier = modifier.height(48.dp)
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .fillMaxWidth()
            .clickable { onClick() }
            .then(
                if (selected)
                    Modifier.background(
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(4.dp)
                    )
                else Modifier
            )
    ) {
        Row {
            Icon(
                imageVector = imageVector,
                contentDescription = contentDescription,
                modifier = Modifier.padding(8.dp)
                    .padding(end = 32.dp),
                tint = if (selected) MaterialTheme.colors.primary else Color.Gray
            )
            Box(
                modifier = Modifier.fillMaxHeight(),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    text = text,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = FontWeight.Medium,
                    color = if (selected) MaterialTheme.colors.primary else Color.Unspecified
                )
            }
        }
    }
}
