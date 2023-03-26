package de.redno.disabledlauncher

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.graphics.drawable.toBitmap
import de.redno.disabledlauncher.common.AndroidUtil
import de.redno.disabledlauncher.model.App
import de.redno.disabledlauncher.service.AppService
import de.redno.disabledlauncher.ui.components.ListItem
import de.redno.disabledlauncher.ui.components.ToolbarComponent
import de.redno.disabledlauncher.ui.theme.DisabledLauncherTheme

class SelectAppsActivity : ComponentActivity() {
    companion object {
        fun <R> registerCallback(
            currentActivity: ComponentActivity,
            convert: (selectedPackages: List<String>) -> List<R>,
            callback: (selectedPackages: List<R>) -> Unit
        ): ActivityResultLauncher<Intent> {
            return currentActivity.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                if (it.resultCode == RESULT_OK) {
                    it.data?.getStringArrayListExtra("selected_packages")?.let {
                        callback(convert(it))
                    }
                }
            }
        }

        fun registerCallback(
            currentActivity: ComponentActivity,
            callback: (selectedApps: List<String>) -> Unit
        ): ActivityResultLauncher<Intent> {
            return registerCallback(currentActivity, { it }, callback)
        }

        fun launch(
            context: Context,
            title: String?,
            selectablePackages: Collection<String>,
            resultLauncher: ActivityResultLauncher<Intent>
        ) {
            Intent(context, SelectAppsActivity::class.java).apply {
                title?.let {
                    putExtra("title", it)
                }
                putStringArrayListExtra("selectable_packages", ArrayList(selectablePackages))
            }.also {
                resultLauncher.launch(it)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val title = intent.getStringExtra("title")
            ?: getString(R.string.app_name)
        val selectablePackages = intent.getStringArrayListExtra("selectable_packages")
            ?.map { AppService.getDetailsForPackage(this, it) }
            ?: emptyList()

        setContent {
            DisabledLauncherTheme {
                // A surface container using the 'background' color from the theme
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colors.background) {
                    val selectedPackageList = remember { mutableStateListOf<String>() }
                    Scaffold(
                        topBar = { ToolbarComponent(title = title) },
                        floatingActionButton = {
                            FloatingActionButton(onClick = {
                                Thread {
                                    if (selectedPackageList.isEmpty()) {
                                        AndroidUtil.asyncToastMakeText(
                                            this,
                                            getString(R.string.no_apps_selected),
                                            Toast.LENGTH_SHORT
                                        )
                                    } else {
                                        Intent().apply {
                                            putStringArrayListExtra("selected_packages", ArrayList(selectedPackageList))
                                        }.also {
                                            setResult(RESULT_OK, it)
                                            finish()
                                        }
                                    }
                                }.start()
                            }) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = getString(R.string.confirm_selected_apps)
                                )
                            }
                        }
                    ) {
                        SelectableAppList(
                            appList = selectablePackages,
                            selectedPackageList = selectedPackageList,
                            modifier = Modifier.padding(it)
                        )
                    }
                }
            }
        }
    }
}


@Preview(showBackground = true, showSystemUi = true)
@Composable
fun SelectAppsPreview() {
    val context = LocalContext.current

    DisabledLauncherTheme {
        val installedApps: List<App> = listOf(
            App(
                "Test",
                "it.test",
                true,
                true,
                context.getDrawable(R.drawable.ic_launcher_background)!!.toBitmap()
            ),
            App(
                "Test B",
                "it.test.2",
                true,
                true,
                context.getDrawable(R.drawable.ic_launcher_background)!!.toBitmap()
            )
        )
        val selectedPackageList = remember { mutableStateListOf<String>() }
        Scaffold(
            topBar = { ToolbarComponent(title = "Preview") },
            floatingActionButton = {
                FloatingActionButton(onClick = {}) {
                    Icon(Icons.Default.Check, contentDescription = null)
                }
            },
            content = {
                SelectableAppList(
                    appList = installedApps,
                    selectedPackageList = selectedPackageList,
                    modifier = Modifier.padding(it)
                )
            }
        )
    }
}

@Composable
fun SelectableAppEntry( // TODO: merge with other AppEntry
    app: App,
    selectedPackageList: SnapshotStateList<String>,
    modifier: Modifier = Modifier
) {
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
        startContent = {
            Checkbox(checked = selectedPackageList.contains(app.packageName), onCheckedChange = null)
        },
        modifier = Modifier.toggleable(
            role = Role.Checkbox,
            value = selectedPackageList.contains(app.packageName),
            onValueChange = {
                if (it) {
                    selectedPackageList.add(app.packageName)
                } else {
                    selectedPackageList.remove(app.packageName)
                }
            }
        )
    )
}

@Composable
fun SelectableAppList(
    appList: List<App>,
    selectedPackageList: SnapshotStateList<String>,
    modifier: Modifier = Modifier
) {
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
                        contentDescription = stringResource(R.string.clear_search)
                    )
                }
            }
        )

        LazyColumn {
            items(items = appList
                .filter { // TODO: prevent being called on every text change
                    val searchTerms = text.trim().lowercase().split(" ")
                    val searchReference = "${it.name.trim().lowercase()} ${it.packageName.trim().lowercase()}"

                    searchTerms.all { searchReference.contains(it) }
                }
                .sortedBy { it.name }
                .sortedBy { !it.isInstalled },
                key = App::packageName
            ) { SelectableAppEntry(it, selectedPackageList) }
        }
    }
}
