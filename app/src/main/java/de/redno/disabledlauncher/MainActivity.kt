package de.redno.disabledlauncher

import android.content.Context
import android.content.Intent
import android.os.Bundle
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
import de.redno.disabledlauncher.common.AndroidUtil
import de.redno.disabledlauncher.model.*
import de.redno.disabledlauncher.model.exception.*
import de.redno.disabledlauncher.service.AppService
import de.redno.disabledlauncher.service.Datasource
import de.redno.disabledlauncher.ui.components.ListItem
import de.redno.disabledlauncher.ui.components.ToolbarComponent
import de.redno.disabledlauncher.ui.theme.DisabledLauncherTheme

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
                        topBar = { ToolbarComponent(title = stringResource(id = R.string.app_name)) },
                        floatingActionButton = {
                            FloatingActionButton(onClick = {
                                Thread {
                                    try {
                                        AppService.disableAllApps(this)
                                    } catch (e: DisabledLauncherException) {
                                        e.getLocalizedMessage(this)?.let {
                                            AndroidUtil.asyncToastMakeText(this, it, Toast.LENGTH_SHORT)
                                        }
                                    }
                                }.start()
                            }) {
                                Icon(
                                    Icons.Default.AppBlocking,
                                    contentDescription = getString(R.string.disable_all_apps)
                                )
                            }
                        },
                        content = {
                            AppList(Datasource.loadAppList(this), Modifier.padding(it))
                        }
                    )
                }
            }
        }
    }
}


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppEntry(app: App, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val sharedPreferences = context.getSharedPreferences(context.packageName, Context.MODE_PRIVATE)

    var dropdownExpanded by remember { mutableStateOf(false) }
    ListItem(
        icon = {
            Image(
                app.icon.asImageBitmap(),
                String.format(stringResource(R.string.app_icon), app.name)
            )
        },
        title = app.name,
        description = app.packageName,
        italicStyle = !app.isEnabled,
        disabledStyle = !app.isInstalled,
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
                            .putExtra("package_name", app.packageName)
                        val shortcutInfo = ShortcutInfoCompat.Builder(context, app.packageName)
                            .setShortLabel(app.name)
                            .setLongLabel(app.name)
                            .setIcon(IconCompat.createWithBitmap(app.icon.asImageBitmap().asAndroidBitmap()))
                            .setIntent(intent)
                            .build()
                        requestPinShortcut(context, shortcutInfo, null)
                    } else {
                        Toast.makeText(
                            context,
                            context.getString(R.string.launcher_not_support_pinned),
                            Toast.LENGTH_LONG
                        ).show()
                    }

                    dropdownExpanded = false
                }) {
                    Text(stringResource(R.string.add_shortcut))
                }
                DropdownMenuItem(onClick = {
                    if (Datasource.removePackage(context, app.packageName)) {
                        dropdownExpanded = false
                        // TODO: refresh app list (+ there are other actions/code locations that need to trigger a list refresh)
                    } else {
                        Toast.makeText(context, context.getString(R.string.couldnt_remove_app), Toast.LENGTH_LONG)
                            .show()
                    }
                }) {
                    Text(stringResource(R.string.remove_app))
                }
            }
        },
        modifier = modifier.combinedClickable(
            onClick = {
                Thread {
                    if (app.isInstalled) {
                        try {
                            AppService.openAppLogic(context, app)
                            if (sharedPreferences.getBoolean("sortAppsByUsage", false)) {
                                Datasource.raisePackage(context, app.packageName)
                            }
                            MainActivity.exit()
                        } catch (e: DisabledLauncherException) {
                            e.getLocalizedMessage(context)?.let {
                                AndroidUtil.asyncToastMakeText(context, it, Toast.LENGTH_SHORT)
                            }
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
                        contentDescription = context.getString(R.string.clear_search)
                    )
                }
            }
        )
        LazyColumn {
            val appList = packageNameList.map {
                AppService.getDetailsForPackage(context, it)
            } // TODO: prevent being called on every text change
            items(items = appList
                .filter {
                    val searchTerms = text.trim().lowercase().split(" ")
                    val searchReference = "${it.name.trim().lowercase()} ${it.packageName.trim().lowercase()}"

                    searchTerms.all { searchReference.contains(it) }
                }
                .sortedBy { !it.isInstalled },
                key = App::packageName
            ) { AppEntry(it) }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun DefaultPreview() {
    DisabledLauncherTheme {
        Scaffold(
            topBar = { ToolbarComponent(title = stringResource(id = R.string.app_name)) },
            floatingActionButton = {
                FloatingActionButton(onClick = {}) {
                    Icon(Icons.Default.AppBlocking, contentDescription = null)
                }
            },
            content = {
                AppList(listOf("de.test.1", "de.test.2", "de.test.3"), Modifier.padding(it))
            }
        )
    }
}
