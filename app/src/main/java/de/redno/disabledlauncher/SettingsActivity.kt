package de.redno.disabledlauncher

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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
                    ToolbarComponent(content = { SettingsList() })
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK) {
            when (requestCode) {
                PICK_LAUNCHABLE_APPS_FILE -> {
                    val uri = data?.data

                    uri?.let {
                        contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION)

                        baseContext.getSharedPreferences("de.redno.disabledlauncher", Context.MODE_PRIVATE)
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
        ToolbarComponent(content = { SettingsList() })
    }
}

@Composable
fun SettingsList(modifier: Modifier = Modifier) {
    val context = LocalContext.current

    Column {
        val currentUri = context.getSharedPreferences("de.redno.disabledlauncher", Context.MODE_PRIVATE)
            .getString("launchableAppsFile", null)
            ?: "Click to choose a file"
        ListEntry(
            title = "Launchable apps file",
            description = currentUri,
            modifier = Modifier.clickable { pickLaunchableAppsFile() }
        )
    }
}
