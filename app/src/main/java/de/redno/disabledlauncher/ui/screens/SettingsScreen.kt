package de.redno.disabledlauncher.ui.screens

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.Scaffold
import androidx.compose.material.Switch
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import de.redno.disabledlauncher.MainActivity
import de.redno.disabledlauncher.R
import de.redno.disabledlauncher.common.AndroidUtil
import de.redno.disabledlauncher.model.App
import de.redno.disabledlauncher.model.exception.DisabledLauncherException
import de.redno.disabledlauncher.service.AppService
import de.redno.disabledlauncher.service.Datasource
import de.redno.disabledlauncher.ui.components.ConditionalDialog
import de.redno.disabledlauncher.ui.components.ListItem
import de.redno.disabledlauncher.ui.components.ToolbarComponent
import de.redno.disabledlauncher.ui.theme.DisabledLauncherTheme

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun SettingsPreview() {
    DisabledLauncherTheme {
        SettingsScreen()
    }
}


@Composable
fun SettingsScreen(modifier: Modifier = Modifier) {
    Scaffold(
        modifier = modifier,
        topBar = { ToolbarComponent(title = stringResource(R.string.settings)) }
    ) {
        SettingsList(Modifier.padding(it))
    }
}

@Composable
private fun SettingsList(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val sharedPreferences = context.getSharedPreferences(context.packageName, Context.MODE_PRIVATE)

    Column(modifier = modifier.verticalScroll(rememberScrollState())) {
        Box {
            val currentUri = sharedPreferences.getString("launchableAppsFile", null)
                ?: stringResource(R.string.launchable_apps_file_description)
            ListItem(
                icon = { Icon(Icons.Default.Description, stringResource(R.string.file_icon)) },
                title = stringResource(R.string.launchable_apps_file_title),
                description = currentUri,
                modifier = Modifier.clickable {
                    val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                        addCategory(Intent.CATEGORY_OPENABLE)
                        addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
                        type = "application/json"
                    }

                    MainActivity.lastObject?.pickLaunchableAppsFileResultLauncher?.launch(intent)
                }
            )
        }

        Box {
            val addAppsTitle = stringResource(R.string.add_apps_title)
            val addAppsDialogOpen = remember { mutableStateOf(false) }
            ListItem(
                icon = { Icon(Icons.Default.AppRegistration, stringResource(R.string.apps_edit_icon)) },
                title = addAppsTitle,
                description = stringResource(R.string.add_apps_description),
                modifier = Modifier.clickable { addAppsDialogOpen.value = true }
            )
            ConditionalDialog(addAppsDialogOpen) {
                val addableApps = AppService.getInstalledPackages(context)
                    .subtract(Datasource.loadAppList(context).toSet())
                    .map { AppService.getDetailsForPackage(context, it) }

                SelectMultipleAppsScreen(
                    title = addAppsTitle,
                    selectableApps = addableApps,
                    onConfirmSelection = {
                        addAppsDialogOpen.value = false

                        if (!Datasource.addPackages(context, it.map(App::packageName))) {
                            AndroidUtil.asyncToastMakeText(
                                context,
                                context.getString(R.string.failed_adding_apps),
                                Toast.LENGTH_SHORT
                            )
                        }
                    }
                )
            }
        }

        Divider()

        Box {
            val initialFallbackToGooglePlay = sharedPreferences.getBoolean("fallbackToGooglePlay", false)
            var fallbackToGooglePlay by remember { mutableStateOf(initialFallbackToGooglePlay) }
            ListItem(
                icon = { Icon(Icons.Default.Shop, stringResource(R.string.store_icon)) },
                title = stringResource(R.string.fallback_googleplay_title),
                description = stringResource(R.string.fallback_googleplay_description),
                endContent = { Switch(checked = fallbackToGooglePlay, onCheckedChange = null) },
                modifier = Modifier.toggleable(
                    role = Role.Switch,
                    value = fallbackToGooglePlay,
                    onValueChange = {
                        sharedPreferences.edit()
                            .putBoolean("fallbackToGooglePlay", it)
                            .apply()

                        fallbackToGooglePlay = it
                    }
                )
            )
        }

        Box {
            val initialSortAppsByUsage = sharedPreferences.getBoolean("sortAppsByUsage", false)
            var sortAppsByUsage by remember { mutableStateOf(initialSortAppsByUsage) }
            ListItem(
                icon = { Icon(Icons.Default.Sort, stringResource(R.string.sort_icon)) },
                title = stringResource(R.string.sort_by_usage_title),
                description = stringResource(R.string.sort_by_usage_description),
                endContent = { Switch(checked = sortAppsByUsage, onCheckedChange = null) },
                modifier = Modifier.toggleable(
                    role = Role.Switch,
                    value = sortAppsByUsage,
                    onValueChange = {
                        sharedPreferences.edit()
                            .putBoolean("sortAppsByUsage", it)
                            .apply()

                        sortAppsByUsage = it
                    }
                )
            )
        }

        Divider()

        Box {
            val disableAppsOnceTitle = stringResource(R.string.disable_apps_once_title)
            val disableAppsOnceDialogOpen = remember { mutableStateOf(false) }
            ListItem( // TODO: move into navigation drawer
                icon = { Icon(Icons.Default.Block, stringResource(R.string.disable_apps_once_icon)) },
                title = disableAppsOnceTitle,
                description = stringResource(R.string.disable_apps_once_description),
                modifier = Modifier.clickable { disableAppsOnceDialogOpen.value = true }
            )
            ConditionalDialog(disableAppsOnceDialogOpen) {
                val enabledApps = AppService.getInstalledPackages(context) { it.applicationInfo.enabled }
                    .map { AppService.getDetailsForPackage(context, it) }

                SelectMultipleAppsScreen(
                    title = disableAppsOnceTitle,
                    selectableApps = enabledApps,
                    onConfirmSelection = {
                        disableAppsOnceDialogOpen.value = false

                        try {
                            it.forEach {
                                AppService.disableApp(context, it, true)
                            }
                        } catch (e: DisabledLauncherException) {
                            e.getLocalizedMessage(context)?.let {
                                AndroidUtil.asyncToastMakeText(context, it, Toast.LENGTH_SHORT)
                            }
                        }
                    }
                )
            }
        }

        Box {
            val enableAppsOnceTitle = stringResource(R.string.enable_apps_once_title)
            val enableAppsOnceDialogOpen = remember { mutableStateOf(false) }
            ListItem( // TODO: move into navigation drawer
                icon = { Icon(Icons.Default.Check, stringResource(R.string.enable_apps_once_icon)) },
                title = enableAppsOnceTitle,
                description = stringResource(R.string.enable_apps_once_description),
                modifier = Modifier.clickable { enableAppsOnceDialogOpen.value = true }
            )
            ConditionalDialog(enableAppsOnceDialogOpen) {
                val disabledApps = AppService.getInstalledPackages(context) { !it.applicationInfo.enabled }
                    .map { AppService.getDetailsForPackage(context, it) }

                SelectMultipleAppsScreen(
                    title = enableAppsOnceTitle,
                    selectableApps = disabledApps,
                    onConfirmSelection = {
                        enableAppsOnceDialogOpen.value = false

                        try {
                            it.forEach {
                                AppService.enableApp(context, it, true)
                            }
                        } catch (e: DisabledLauncherException) {
                            e.getLocalizedMessage(context)?.let {
                                AndroidUtil.asyncToastMakeText(context, it, Toast.LENGTH_SHORT)
                            }
                        }
                    }
                )
            }
        }
    }
}
