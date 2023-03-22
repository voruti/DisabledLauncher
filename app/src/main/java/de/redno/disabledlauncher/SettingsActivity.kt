package de.redno.disabledlauncher

import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import de.redno.disabledlauncher.common.ListEntry
import de.redno.disabledlauncher.service.Datasource
import de.redno.disabledlauncher.ui.theme.DisabledLauncherTheme

class SettingsActivity : ComponentActivity() {
    companion object {
        var lastObject: SettingsActivity? = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lastObject = this
        setContent {
            DisabledLauncherTheme {
                // A surface container using the 'background' color from the theme
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colors.background) {
                    Scaffold(
                        topBar = { ToolbarComponent(showSettings = false) },
                        content = { padding ->
                            SettingsList(Modifier.padding(padding))
                        }
                    )
                }
            }
        }
    }

    val pickLaunchableAppsFileResultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                result.data?.data?.let {
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

    val addAppsResultLauncher = SelectAppsActivity.registerCallback(this) { selectedPackages ->
        if (!Datasource.addPackages(this, selectedPackages)) {
            asyncToastMakeText(
                this,
                getString(R.string.failed_adding_apps),
                Toast.LENGTH_SHORT
            )
        }
    }

    val disableAppsOnceResultLauncher = SelectAppsActivity.registerCallback(this) { selectedPackages ->
        selectedPackages.forEach {
            disableApp(this, it)
        }
    }
}


fun getInstalledPackages(
    context: Context,
    packageInfoFilter: (packageInfo: PackageInfo) -> Boolean = { _ -> true }
): List<String> {
    val packageManager = context.packageManager

    return packageManager.getInstalledPackages(PackageManager.GET_META_DATA)
        .filter(packageInfoFilter)
        .map { packageInfo -> packageInfo.packageName }
}


@Preview(showBackground = true, showSystemUi = true)
@Composable
fun SettingsPreview() {
    DisabledLauncherTheme {
        Scaffold(
            topBar = { ToolbarComponent(showSettings = false) },
            content = { padding ->
                SettingsList(Modifier.padding(padding))
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
        ListEntry(
            icon = { Icon(Icons.Default.Description, stringResource(R.string.file_icon)) },
            title = stringResource(R.string.launchable_apps_file_title),
            description = currentUri,
            modifier = Modifier.clickable {
                val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
                    type = "application/json"
                }

                SettingsActivity.lastObject?.pickLaunchableAppsFileResultLauncher?.launch(intent)
            }
        )

        ListEntry(
            icon = { Icon(Icons.Default.AppRegistration, stringResource(R.string.apps_edit_icon)) },
            title = stringResource(R.string.add_apps_title),
            description = stringResource(R.string.add_apps_description),
            modifier = Modifier.clickable {
                val addableApps = getInstalledPackages(context)
                    .subtract(Datasource.loadAppList(context).toSet())

                SettingsActivity.lastObject?.addAppsResultLauncher?.let {
                    SelectAppsActivity.launch(context, addableApps, it)
                }
            }
        )

        Divider()

        val initialFallbackToGooglePlay = sharedPreferences.getBoolean("fallbackToGooglePlay", false)
        var fallbackToGooglePlay by remember { mutableStateOf(initialFallbackToGooglePlay) }
        ListEntry(
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
        ListEntry(
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

        ListEntry( // TODO: move into navigation drawer
            icon = { Icon(Icons.Default.Block, stringResource(R.string.disable_apps_once_icon)) },
            title = stringResource(R.string.disable_apps_once_title),
            description = stringResource(R.string.disable_apps_once_description),
            modifier = Modifier.clickable {
                val enabledApps = getInstalledPackages(context) { packageInfo ->
                    packageInfo.applicationInfo.enabled
                }

                SettingsActivity.lastObject?.disableAppsOnceResultLauncher?.let {
                    SelectAppsActivity.launch(context, enabledApps, it)
                }
            }
        )
    }
}
