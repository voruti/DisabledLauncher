package de.redno.disabledlauncher.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.graphics.drawable.toBitmap
import de.redno.disabledlauncher.R
import de.redno.disabledlauncher.common.AndroidUtil
import de.redno.disabledlauncher.model.App
import de.redno.disabledlauncher.ui.components.AppEntry
import de.redno.disabledlauncher.ui.components.ToolbarComponent
import de.redno.disabledlauncher.ui.theme.DisabledLauncherTheme

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun SelectMultipleAppsPreview() {
    val context = LocalContext.current

    DisabledLauncherTheme {
        SelectMultipleAppsScreen(
            title = "Preview",
            selectableApps = listOf(
                App(
                    name = "P1",
                    packageName = "test.p1",
                    isEnabled = true,
                    isInstalled = true,
                    icon = context.getDrawable(R.drawable.ic_launcher_background)!!
                        .toBitmap()
                )
            ),
            onConfirmSelection = {},
            onBackNavigation = {}
        )
    }
}


@Composable
fun SelectMultipleAppsScreen(
    title: String,
    selectableApps: List<App>,
    onConfirmSelection: (List<App>) -> Unit,
    modifier: Modifier = Modifier,
    onMenuClick: (() -> Unit)? = null,
    onBackNavigation: (() -> Unit)? = null
) {
    val context = LocalContext.current

    val selectedAppList = remember { mutableStateListOf<App>() }
    Scaffold(
        modifier = modifier,
        topBar = {
            ToolbarComponent(
                title = title,
                onMenuClick = onMenuClick,
                onBackNavigation = onBackNavigation
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                Thread {
                    if (selectedAppList.isEmpty()) {
                        AndroidUtil.asyncToastMakeText(
                            context,
                            context.getString(R.string.no_apps_selected),
                            Toast.LENGTH_SHORT
                        )
                    } else {
                        onConfirmSelection(ArrayList(selectedAppList))
                        selectedAppList.clear()
                    }
                }.start()
            }) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = stringResource(R.string.confirm_selected_apps)
                )
            }
        }
    ) {
        SelectableAppList(
            appList = selectableApps,
            selectedAppList = selectedAppList,
            modifier = Modifier.padding(it)
        )
    }
}

@Composable
private fun SelectableAppList(
    appList: List<App>,
    selectedAppList: SnapshotStateList<App>,
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
            ) {
                AppEntry(
                    app = it,
                    selectedAppList = selectedAppList
                )
            }
        }
    }
}
