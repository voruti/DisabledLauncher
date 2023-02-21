package de.redno.disabledlauncher

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AppBlocking
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.graphics.drawable.toBitmap
import de.redno.disabledlauncher.common.ListEntry
import de.redno.disabledlauncher.data.Datasource
import de.redno.disabledlauncher.model.*
import de.redno.disabledlauncher.ui.theme.DisabledLauncherTheme
import rikka.shizuku.Shizuku

class MainActivity : ComponentActivity() {
    companion object {
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
                // A surface container using the 'background' color from the theme
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colors.background) {
                    Scaffold(
                        topBar = { ToolbarComponent() },
                        floatingActionButton = {
                            FloatingActionButton(onClick = {
                                Thread {
                                    if (!disableAllApps(baseContext)) {
                                        asyncToastMakeText(baseContext, "Couldn't disable all apps", Toast.LENGTH_LONG)
                                    }
                                }.start()
                            }) {
                                Icon(Icons.Default.AppBlocking, contentDescription = "Disable all apps")
                            }
                        },
                        content = { padding ->
                            AppList(Datasource().loadAppList(baseContext), Modifier.padding(padding))
                        }
                    )
                }
            }
        }
    }
}


fun checkShizukuPermission(): Boolean {
    try {
        if (Shizuku.isPreV11()) {
            // Pre-v11 is unsupported
            throw ShizukuVersionNotSupportedException()
        } else if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
            // Granted
            return true
        } else if (!Shizuku.shouldShowRequestPermissionRationale()) {
            // Users choose "Deny and don't ask again"
            throw NoShizukuPermissionException()
        } else {
            // Request the permission
            Shizuku.requestPermission(0)
            throw NoShizukuPermissionException()
        }
    } catch (e: IllegalStateException) {
        throw ShizukuUnavailableException()
    }
}

fun getDetailsForPackage(context: Context, packageName: String): AppEntryInList {
    val packageManager = context.packageManager

    try {
        packageManager.getPackageInfo(packageName, 0)?.let { packageInfo ->
            return AppEntryInList(
                packageInfo.applicationInfo.loadLabel(packageManager).toString(),
                packageInfo.packageName,
                packageInfo.applicationInfo.enabled,
                true,
                packageInfo.applicationInfo.loadIcon(packageManager).toBitmap()
            )
        }
    } catch (e: PackageManager.NameNotFoundException) {
        e.printStackTrace()
    }

    return AppEntryInList(
        name = "App not found",
        packageName = packageName,
        icon = context.getDrawable(R.drawable.ic_launcher_background)!!.toBitmap(),
        isEnabled = false,
        isInstalled = false
    )
}

fun enableApp(context: Context, packageName: String): Boolean {
    try {
        return executeAdbCommand("pm enable $packageName")
    } catch (e: ShizukuException) {
        val sharedPreferences = context.getSharedPreferences(context.packageName, Context.MODE_PRIVATE)
        val fallbackToGooglePlay = sharedPreferences.getBoolean("fallbackToGooglePlay", false)

        if (fallbackToGooglePlay) {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("https://play.google.com/store/apps/details?id=$packageName")
            }
            context.startActivity(intent)
        }

        throw e
    }
}

fun disableAllApps(context: Context): Boolean {
    val packagesToDisable = Datasource().loadAppList(context)
        .filter { packageName -> getDetailsForPackage(context, packageName).isEnabled }

    for (packageName in packagesToDisable) {
        if (!disableApp(context, packageName)) {
            return false
        }
    }

    return true
}

fun disableApp(context: Context, packageName: String): Boolean {
    val success = executeAdbCommand("pm disable-user --user 0 $packageName")

    if (success) {
        asyncToastMakeText(context, "Disabled $packageName", Toast.LENGTH_SHORT)
    }

    return success
}

fun executeAdbCommand(command: String): Boolean {
    if (!checkShizukuPermission()) {
        return false
    }

    return Shizuku.newProcess(arrayOf("sh", "-c", command), null, null).waitFor() == 0
}

fun startApp(context: Context, packageName: String): Boolean {
    return try {
        val launchIntent = context.packageManager.getLaunchIntentForPackage(packageName)
            ?.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
        context.startActivity(launchIntent)

        true
    } catch (e: Exception) {
        false
    }
}

fun asyncToastMakeText(context: Context, text: CharSequence, duration: Int) {
    Handler(Looper.getMainLooper()).post {
        Toast.makeText(context, text, duration).show()
    }
}


@Composable
fun ToolbarComponent(modifier: Modifier = Modifier) {
    val context = LocalContext.current

    TopAppBar(
        modifier = modifier,
        title = { Text(text = stringResource(id = R.string.app_name)) },
        actions = {
            IconButton(
                onClick = { context.startActivity(Intent(context, SettingsActivity::class.java)) }) {
                Icon(Icons.Default.Settings, contentDescription = stringResource(id = R.string.settings))
            }
        }
    )
}

@Composable
fun AppEntry(appEntry: AppEntryInList, modifier: Modifier = Modifier) {
    val context = LocalContext.current

    ListEntry(
        icon = { Image(appEntry.icon.asImageBitmap(), "App icon") },
        title = appEntry.name,
        description = appEntry.packageName,
        italicStyle = !appEntry.isEnabled,
        disabledStyle = !appEntry.isInstalled,
        modifier = modifier.clickable {
            Thread {
                try {
                    if (appEntry.isEnabled || enableApp(context, appEntry.packageName)) {
                        if (startApp(context, appEntry.packageName)) {
                            MainActivity.exit()
                        }
                    } else {
                        asyncToastMakeText(context, "App can't be opened", Toast.LENGTH_SHORT)
                    }
                } catch (e: ShizukuUnavailableException) {
                    asyncToastMakeText(context, "Can't connect to Shizuku", Toast.LENGTH_SHORT)
                } catch (e: NoShizukuPermissionException) {
                    asyncToastMakeText(context, "Shizuku denied access", Toast.LENGTH_SHORT)
                } catch (e: ShizukuVersionNotSupportedException) {
                    asyncToastMakeText(context, "Unsupported Shizuku version", Toast.LENGTH_LONG)
                }
            }.start()
        }
    )
}

@Composable
fun AppList(packageNameList: List<String>, modifier: Modifier = Modifier) {
    val context = LocalContext.current

    Column(modifier = modifier) {
        var text by rememberSaveable { mutableStateOf("") }
        TextField( // TODO: https://developer.android.com/jetpack/compose/text#enter-modify-text
            value = text, // TODO: put into toolbar
            onValueChange = { text = it },
            label = { Text(stringResource(id = R.string.search)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            trailingIcon = {
                IconButton(
                    onClick = { text = "" }
                ) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = "Clear search"
                    )
                }
            }
        )
        LazyColumn {
            val appEntryList = packageNameList.map {
                getDetailsForPackage(context, it)
            } // TODO: prevent being called on every text change
            items(items = appEntryList
                .filter {
                    val searchTerms = text.trim().lowercase().split(" ")
                    val searchReference = "${it.name.trim().lowercase()} ${it.packageName.trim().lowercase()}"

                    searchTerms.all { searchTerm -> searchReference.contains(searchTerm) }
                }
                .sortedBy { !it.isInstalled },
                key = { it.packageName }
            ) { AppEntry(it) }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun DefaultPreview() {
    DisabledLauncherTheme {
        Scaffold(
            topBar = { ToolbarComponent() },
            floatingActionButton = {
                FloatingActionButton(onClick = {}) {
                    Icon(Icons.Default.AppBlocking, contentDescription = "Disable all apps")
                }
            },
            content = { padding ->
                AppList(listOf("de.test.1", "de.test.2", "de.test.3"), Modifier.padding(padding))
            }
        )
    }
}
