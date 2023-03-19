package de.redno.disabledlauncher

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import de.redno.disabledlauncher.common.ListEntry
import de.redno.disabledlauncher.model.AppEntryInList
import de.redno.disabledlauncher.ui.theme.DisabledLauncherTheme

class AddAppsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val selectablePackages = intent.getStringArrayListExtra("selectable_packages")
            ?.map { packageString -> getDetailsForPackage(this, packageString) }
            ?: emptyList()

        setContent {
            DisabledLauncherTheme {
                // A surface container using the 'background' color from the theme
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colors.background) {
                    val selectedPackageList = remember { mutableStateListOf<String>() }
                    Scaffold(
                        topBar = { ToolbarComponent() },
                        floatingActionButton = {
                            FloatingActionButton(onClick = {
                                Thread {
                                    if (selectedPackageList.isEmpty()) {
                                        asyncToastMakeText(
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
                    ) { padding ->
                        SelectableAppList(
                            appEntryList = selectablePackages,
                            selectedPackageList = selectedPackageList,
                            modifier = Modifier.padding(padding)
                        )
                    }
                }
            }
        }
    }
}


@Preview(showBackground = true, showSystemUi = true)
@Composable
fun AddAppsPreview() {
    val context = LocalContext.current

    DisabledLauncherTheme {
        val installedApps: List<AppEntryInList> = listOf(
            AppEntryInList(
                "Test",
                "it.test",
                true,
                true,
                context.getDrawable(R.drawable.ic_launcher_background)!!.toBitmap()
            ),
            AppEntryInList(
                "Test B",
                "it.test.2",
                true,
                true,
                context.getDrawable(R.drawable.ic_launcher_background)!!.toBitmap()
            )
        )
        val selectedPackageList = remember { mutableStateListOf<String>() }
        Scaffold(
            topBar = { ToolbarComponent() },
            floatingActionButton = {
                FloatingActionButton(onClick = {}) {
                    Icon(Icons.Default.Check, contentDescription = null)
                }
            },
            content = { padding ->
                SelectableAppList(
                    appEntryList = installedApps,
                    selectedPackageList = selectedPackageList,
                    modifier = Modifier.padding(padding)
                )
            }
        )
    }
}

@Composable
fun SelectableAppEntry(
    appEntry: AppEntryInList,
    selectedPackageList: SnapshotStateList<String>,
    modifier: Modifier = Modifier
) {
    ListEntry(
        icon = {
            Image(
                appEntry.icon.asImageBitmap(),
                String.format(stringResource(R.string.app_icon), appEntry.name)
            )
        },
        title = appEntry.name,
        description = appEntry.packageName,
        italicStyle = !appEntry.isEnabled,
        disabledStyle = !appEntry.isInstalled,
        startContent = {
            Checkbox(checked = selectedPackageList.contains(appEntry.packageName), onCheckedChange = null)
        },
        modifier = Modifier.toggleable(
            role = Role.Checkbox,
            value = selectedPackageList.contains(appEntry.packageName),
            onValueChange = {
                if (it) {
                    selectedPackageList.add(appEntry.packageName)
                } else {
                    selectedPackageList.remove(appEntry.packageName)
                }
            }
        )
    )
}

@Composable
fun SelectableAppList(
    appEntryList: List<AppEntryInList>,
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
            items(items = appEntryList
                .filter { // TODO: prevent being called on every text change
                    val searchTerms = text.trim().lowercase().split(" ")
                    val searchReference = "${it.name.trim().lowercase()} ${it.packageName.trim().lowercase()}"

                    searchTerms.all { searchTerm -> searchReference.contains(searchTerm) }
                }
                .sortedBy { it.name }
                .sortedBy { !it.isInstalled }
            ) { SelectableAppEntry(it, selectedPackageList) }
        }
    }
}
