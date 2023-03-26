package de.redno.disabledlauncher.ui.components

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.clickable
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
import de.redno.disabledlauncher.SelectAppsActivity
import de.redno.disabledlauncher.service.AppService
import de.redno.disabledlauncher.service.Datasource
import de.redno.disabledlauncher.ui.theme.DisabledLauncherTheme

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun SettingsPreview() {
    DisabledLauncherTheme {
        Scaffold(
            topBar = { ToolbarComponent(title = stringResource(R.string.settings)) },
            content = {
                SettingsList(Modifier.padding(it))
            }
        )
    }
}


@Composable
fun SettingsList(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val sharedPreferences = context.getSharedPreferences(context.packageName, Context.MODE_PRIVATE)

    Column(modifier = modifier.verticalScroll(rememberScrollState())) {
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

        val addAppsTitle = stringResource(R.string.add_apps_title)
        ListItem(
            icon = { Icon(Icons.Default.AppRegistration, stringResource(R.string.apps_edit_icon)) },
            title = addAppsTitle,
            description = stringResource(R.string.add_apps_description),
            modifier = Modifier.clickable {
                val addableApps = AppService.getInstalledPackages(context)
                    .subtract(Datasource.loadAppList(context).toSet())

                MainActivity.lastObject?.addAppsResultLauncher?.let {
                    SelectAppsActivity.launch(context, addAppsTitle, addableApps, it)
                }
            }
        )

        Divider()

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

        Divider()

        val disableAppsOnceTitle = stringResource(R.string.disable_apps_once_title)
        ListItem( // TODO: move into navigation drawer
            icon = { Icon(Icons.Default.Block, stringResource(R.string.disable_apps_once_icon)) },
            title = disableAppsOnceTitle,
            description = stringResource(R.string.disable_apps_once_description),
            modifier = Modifier.clickable {
                val enabledApps = AppService.getInstalledPackages(context) {
                    it.applicationInfo.enabled
                }

                MainActivity.lastObject?.disableAppsOnceResultLauncher?.let {
                    SelectAppsActivity.launch(context, disableAppsOnceTitle, enabledApps, it)
                }
            }
        )

        val enableAppsOnceTitle = stringResource(R.string.enable_apps_once_title)
        ListItem( // TODO: move into navigation drawer
            icon = { Icon(Icons.Default.Check, stringResource(R.string.enable_apps_once_icon)) },
            title = enableAppsOnceTitle,
            description = stringResource(R.string.enable_apps_once_description),
            modifier = Modifier.clickable {
                val disabledApps = AppService.getInstalledPackages(context) {
                    !it.applicationInfo.enabled
                }

                MainActivity.lastObject?.enableAppsOnceResultLauncher?.let {
                    SelectAppsActivity.launch(context, enableAppsOnceTitle, disabledApps, it)
                }
            }
        )
    }
}
