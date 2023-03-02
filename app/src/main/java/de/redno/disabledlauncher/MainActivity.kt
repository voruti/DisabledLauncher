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
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat.isRequestPinShortcutSupported
import androidx.core.content.pm.ShortcutManagerCompat.requestPinShortcut
import androidx.core.graphics.drawable.IconCompat
import androidx.core.graphics.drawable.toBitmap
import de.redno.disabledlauncher.common.ListEntry
import de.redno.disabledlauncher.model.*
import de.redno.disabledlauncher.model.exception.*
import de.redno.disabledlauncher.service.Datasource
import de.redno.disabledlauncher.ui.theme.DisabledLauncherTheme
import rikka.shizuku.Shizuku

class MainActivity : ComponentActivity() { // TODO: faster startup somehow?
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
                                    try {
                                        disableAllApps(this)
                                    } catch (e: DisabledLauncherException) {
                                        e.message?.let {
                                            asyncToastMakeText(this, it, Toast.LENGTH_SHORT)
                                        }
                                    }
                                }.start()
                            }) {
                                Icon(Icons.Default.AppBlocking, contentDescription = "Disable all apps")
                            }
                        },
                        content = { padding ->
                            AppList(Datasource.loadAppList(this), Modifier.padding(padding))
                        }
                    )
                }
            }
        }
    }
}


@Throws(ShizukuException::class)
fun checkShizukuPermission() {
    try {
        if (Shizuku.isPreV11()) {
            // Pre-v11 is unsupported
            throw ShizukuVersionNotSupportedException()
        } else if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
            // Granted
            return
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

@Throws(DisabledLauncherException::class)
fun enableApp(context: Context, packageName: String) {
    try {
        executeAdbCommand("pm enable $packageName")
    } catch (e: DisabledLauncherException) {
        val sharedPreferences = context.getSharedPreferences(context.packageName, Context.MODE_PRIVATE)
        val fallbackToGooglePlay = sharedPreferences.getBoolean("fallbackToGooglePlay", false)

        if (fallbackToGooglePlay) {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("https://play.google.com/store/apps/details?id=$packageName")
            }
            context.startActivity(intent)
            throw RedirectedToGooglePlayException(e.message)
        }

        throw e
    }
}

@Throws(DisabledLauncherException::class)
fun disableAllApps(context: Context) {
    val packagesToDisable = Datasource.loadAppList(context)
        .filter { packageName -> getDetailsForPackage(context, packageName).isEnabled }

    if (packagesToDisable.isEmpty()) {
        asyncToastMakeText(context, "Nothing to disable", Toast.LENGTH_SHORT)
        return
    }

    for (packageName in packagesToDisable) {
        disableApp(context, packageName)
    }
}

@Throws(DisabledLauncherException::class)
fun disableApp(context: Context, packageName: String) {
    executeAdbCommand("pm disable-user --user 0 $packageName")

    asyncToastMakeText(context, "Disabled $packageName", Toast.LENGTH_SHORT)
}

@Throws(DisabledLauncherException::class)
fun executeAdbCommand(command: String) {
    checkShizukuPermission()

    if (Shizuku.newProcess(arrayOf("sh", "-c", command), null, null).waitFor() != 0) {
        throw DisabledLauncherException("Process failure")
    }
}

@Throws(DisabledLauncherException::class)
fun openAppLogic(context: Context, appEntry: AppEntryInList) {
    if (!appEntry.isEnabled) {
        enableApp(context, appEntry.packageName)
    }

    startApp(context, appEntry.packageName)
}

@Throws(DisabledLauncherException::class)
fun startApp(context: Context, packageName: String) {
    try {
        val launchIntent = context.packageManager.getLaunchIntentForPackage(packageName)
            ?.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
        context.startActivity(launchIntent)
    } catch (e: Exception) {
        throw DisabledLauncherException("App can't be opened")
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppEntry(appEntry: AppEntryInList, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val sharedPreferences = context.getSharedPreferences(context.packageName, Context.MODE_PRIVATE)

    var dropdownExpanded by remember { mutableStateOf(false) }
    ListEntry(
        icon = { Image(appEntry.icon.asImageBitmap(), "App icon") },
        title = appEntry.name,
        description = appEntry.packageName,
        italicStyle = !appEntry.isEnabled,
        disabledStyle = !appEntry.isInstalled,
        contextContent = {
            DropdownMenu(
                expanded = dropdownExpanded,
                onDismissRequest = { dropdownExpanded = false }
            ) {
                DropdownMenuItem(onClick = {
                    if (isRequestPinShortcutSupported(context)) {
                        val intent = Intent(context, ActionReceiverActivity::class.java)
                            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            .setAction("${context.packageName}.action.OPEN_APP")
                            .putExtra("package_name", appEntry.packageName)
                        val shortcutInfo = ShortcutInfoCompat.Builder(context, appEntry.packageName)
                            .setShortLabel(appEntry.name)
                            .setLongLabel(appEntry.name)
                            .setIcon(IconCompat.createWithBitmap(appEntry.icon.asImageBitmap().asAndroidBitmap()))
                            .setIntent(intent)
                            .build()
                        requestPinShortcut(context, shortcutInfo, null)
                    } else {
                        Toast.makeText(context, "Launcher doesn't support pinned shortcuts", Toast.LENGTH_LONG)
                            .show()
                    }

                    dropdownExpanded = false
                }) {
                    Text("Add shortcut to home screen")
                }
                DropdownMenuItem(onClick = {
                    if (Datasource.removePackage(context, appEntry.packageName)) {
                        dropdownExpanded = false
                    } else {
                        Toast.makeText(context, "Couldn't remove app", Toast.LENGTH_LONG).show()
                    }
                }) {
                    Text("Remove app")
                }
            }
        },
        modifier = modifier.combinedClickable(
            onClick = {
                Thread {
                    try {
                        openAppLogic(context, appEntry)
                        if (sharedPreferences.getBoolean("sortAppsByUsage", false)) {
                            Datasource.raisePackage(context, appEntry.packageName)
                        }
                        MainActivity.exit()
                    } catch (e: DisabledLauncherException) {
                        e.message?.let {
                            asyncToastMakeText(context, it, Toast.LENGTH_SHORT)
                        }
                    }
                }.start()
            },
            onLongClick = { dropdownExpanded = true }
        )
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
