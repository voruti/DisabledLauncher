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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.AppRegistration
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Shop
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.edit
import de.redno.disabledlauncher.MainActivity
import de.redno.disabledlauncher.R
import de.redno.disabledlauncher.common.AndroidUtil
import de.redno.disabledlauncher.model.App
import de.redno.disabledlauncher.model.ListType
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
        SettingsScreen(
            onBackNavigation = {})
    }
}


@Composable
fun SettingsScreen(
    onBackNavigation: () -> Unit, modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier, topBar = {
            ToolbarComponent(
                title = stringResource(R.string.settings), onBackNavigation = onBackNavigation
            )
        }) {
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
                })
        }

        val textFailedAddingApps: String = stringResource(R.string.failed_adding_apps)
        Box {
            val addDirectAppsTitle = stringResource(R.string.add_direct_apps_title)
            val addDirectAppsDialogOpen = remember { mutableStateOf(false) }
            ListItem(
                icon = {
                    Icon(
                        Icons.Default.AppRegistration, stringResource(R.string.apps_edit_icon)
                    )
                },
                title = addDirectAppsTitle,
                description = stringResource(R.string.add_direct_apps_description),
                modifier = Modifier.clickable { addDirectAppsDialogOpen.value = true })
            ConditionalDialog(addDirectAppsDialogOpen) {
                val addableApps = AppService.getInstalledPackages(context)
                    .subtract(Datasource.loadAppList(context, ListType.DIRECT).toSet())
                    .map { AppService.getDetailsForPackage(context, it) }

                SelectMultipleAppsScreen(
                    title = addDirectAppsTitle,
                    selectableApps = addableApps,
                    onConfirmSelection = {
                        addDirectAppsDialogOpen.value = false

                        if (!Datasource.addPackages(
                                context, it.map(App::packageName), ListType.DIRECT
                            )
                        ) {
                            AndroidUtil.asyncToastMakeText(
                                context,
                                textFailedAddingApps,
                                Toast.LENGTH_SHORT
                            )
                        }
                    },
                    onBackNavigation = { addDirectAppsDialogOpen.value = false })
            }
        }

        Box {
            val addLongTermAppsTitle = stringResource(R.string.add_long_term_apps_title)
            val addLongTermAppsDialogOpen = remember { mutableStateOf(false) }
            ListItem(
                icon = {
                    Icon(
                        Icons.Default.AppRegistration, stringResource(R.string.apps_edit_icon)
                    )
                },
                title = addLongTermAppsTitle,
                description = stringResource(R.string.add_long_term_apps_description),
                modifier = Modifier.clickable { addLongTermAppsDialogOpen.value = true })
            ConditionalDialog(addLongTermAppsDialogOpen) {
                val addableApps = AppService.getInstalledPackages(context)
                    .subtract(Datasource.loadAppList(context, ListType.LONG_TERM).toSet())
                    .map { AppService.getDetailsForPackage(context, it) }

                SelectMultipleAppsScreen(
                    title = addLongTermAppsTitle,
                    selectableApps = addableApps,
                    onConfirmSelection = {
                        addLongTermAppsDialogOpen.value = false

                        if (!Datasource.addPackages(
                                context, it.map(App::packageName), ListType.LONG_TERM
                            )
                        ) {
                            AndroidUtil.asyncToastMakeText(
                                context,
                                textFailedAddingApps,
                                Toast.LENGTH_SHORT
                            )
                        }
                    },
                    onBackNavigation = { addLongTermAppsDialogOpen.value = false })
            }
        }

        HorizontalDivider()

        Box {
            val initialFallbackToGooglePlay =
                sharedPreferences.getBoolean("fallbackToGooglePlay", false)
            var fallbackToGooglePlay by remember { mutableStateOf(initialFallbackToGooglePlay) }
            ListItem(
                icon = { Icon(Icons.Default.Shop, stringResource(R.string.store_icon)) },
                title = stringResource(R.string.fallback_googleplay_title),
                description = stringResource(R.string.fallback_googleplay_description),
                endContent = { Switch(checked = fallbackToGooglePlay, onCheckedChange = null) },
                modifier = Modifier.toggleable(
                    role = Role.Switch, value = fallbackToGooglePlay, onValueChange = {
                        sharedPreferences.edit {
                            putBoolean(
                                "fallbackToGooglePlay",
                                it
                            )
                        }

                        fallbackToGooglePlay = it
                    })
            )
        }

        Box {
            val initialSortAppsByUsage = sharedPreferences.getBoolean("sortAppsByUsage", false)
            var sortAppsByUsage by remember { mutableStateOf(initialSortAppsByUsage) }
            ListItem(
                icon = {
                    Icon(
                        Icons.AutoMirrored.Filled.Sort, stringResource(R.string.sort_icon)
                    )
                },
                title = stringResource(R.string.sort_by_usage_title),
                description = stringResource(R.string.sort_by_usage_description),
                endContent = { Switch(checked = sortAppsByUsage, onCheckedChange = null) },
                modifier = Modifier.toggleable(
                    role = Role.Switch, value = sortAppsByUsage, onValueChange = {
                        sharedPreferences.edit {
                            putBoolean(
                                "sortAppsByUsage",
                                it
                            )
                        }

                        sortAppsByUsage = it
                    })
            )
        }
    }
}
