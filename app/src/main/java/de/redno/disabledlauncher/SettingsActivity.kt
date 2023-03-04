package de.redno.disabledlauncher

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AppRegistration
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Shop
import androidx.compose.material.icons.filled.Sort
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.app.ActivityCompat.startActivityForResult
import de.redno.disabledlauncher.common.ListEntry
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
                        topBar = { ToolbarComponent() },
                        content = { padding ->
                            SettingsList(Modifier.padding(padding))
                        }
                    )
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK) {
            when (requestCode) {
                PICK_LAUNCHABLE_APPS_FILE -> {
                    data?.data?.let {
                        contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_WRITE_URI_PERMISSION)

                        getSharedPreferences(packageName, Context.MODE_PRIVATE)
                            .edit()
                            .putString("launchableAppsFile", it.toString())
                            .apply()
                    }
                }
            }
        }
    }
}


/**
 * Request code for selecting a json file with launch-able app packages.
 */
const val PICK_LAUNCHABLE_APPS_FILE = 1
fun pickLaunchableAppsFile() {
    val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
        addCategory(Intent.CATEGORY_OPENABLE)
        addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
        type = "application/json"
    }

    SettingsActivity.lastObject?.let {
        startActivityForResult(it, intent, PICK_LAUNCHABLE_APPS_FILE, null)
    }
}


@Preview(showBackground = true, showSystemUi = true)
@Composable
fun SettingsPreview() {
    DisabledLauncherTheme {
        Scaffold(
            topBar = { ToolbarComponent() },
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

    Column(modifier = modifier) {
        val currentUri = sharedPreferences.getString("launchableAppsFile", null)
            ?: "Click to choose a file"
        ListEntry(
            icon = { Icon(Icons.Default.Description, "File icon") },
            title = "Launchable apps file",
            description = currentUri,
            modifier = Modifier.clickable { pickLaunchableAppsFile() }
        )

        ListEntry(
            icon = { Icon(Icons.Default.AppRegistration, "Apps edit icon") },
            title = "Add apps",
            description = "Add apps to the launchable apps file",
            modifier = Modifier.clickable {
                context.startActivity(Intent(context, AddAppsActivity::class.java))
            }
        )

        Divider()

        val initialFallbackToGooglePlay = sharedPreferences.getBoolean("fallbackToGooglePlay", false)
        var fallbackToGooglePlay by remember { mutableStateOf(initialFallbackToGooglePlay) }
        ListEntry(
            icon = { Icon(Icons.Default.Shop, "Store icon") },
            title = "Fallback to Google Play",
            description = "When ADB/Shizuku isn't available, redirect the user to Google Play",
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
            icon = { Icon(Icons.Default.Sort, "Sort icon") },
            title = "Sort apps by usage",
            description = "Move apps up in the list when they are opened",
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
}
