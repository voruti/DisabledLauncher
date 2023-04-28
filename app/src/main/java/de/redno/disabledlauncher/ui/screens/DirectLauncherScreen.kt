package de.redno.disabledlauncher.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AppBlocking
import androidx.compose.material.icons.filled.Clear
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import de.redno.disabledlauncher.R
import de.redno.disabledlauncher.common.AndroidUtil
import de.redno.disabledlauncher.model.App
import de.redno.disabledlauncher.model.ListType
import de.redno.disabledlauncher.model.exception.DisabledLauncherException
import de.redno.disabledlauncher.service.AppService
import de.redno.disabledlauncher.ui.components.AppEntry
import de.redno.disabledlauncher.ui.components.ToolbarComponent
import de.redno.disabledlauncher.ui.theme.DisabledLauncherTheme

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun DirectLauncherPreview() {
    DisabledLauncherTheme {
        DirectLauncherScreen(
            onMenuClick = {},
            packageNameList = listOf("de.test.1", "de.test.2", "de.test.3")
        )
    }
}


@Composable
fun DirectLauncherScreen(
    onMenuClick: () -> Unit,
    packageNameList: List<String>,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Scaffold(
        modifier = modifier,
        topBar = {
            ToolbarComponent(
                title = stringResource(id = R.string.direct_launcher_title),
                onMenuClick = onMenuClick
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = Thread {
                try {
                    AppService.disableAllApps(context)
                } catch (e: DisabledLauncherException) {
                    e.getLocalizedMessage(context)?.let {
                        AndroidUtil.asyncToastMakeText(context, it, Toast.LENGTH_SHORT)
                    }
                }
            }::start) {
                Icon(
                    Icons.Default.AppBlocking,
                    contentDescription = stringResource(R.string.disable_all_apps)
                )
            }
        }
    ) {
        AppList(
            appList = packageNameList.map {
                AppService.getDetailsForPackage(context, it, ListType.DIRECT)
            },
            modifier = Modifier.padding(it)
        )
    }
}

@Composable
fun AppList(
    appList: List<App>,
    modifier: Modifier = Modifier
) {
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
            // TODO: prevent being called on every search text change (; is this still applicable?)
            items(items = appList
                .filter {
                    val searchTerms = text.trim().lowercase().split(" ")
                    val searchReference = "${it.name.trim().lowercase()} ${it.packageName.trim().lowercase()}"

                    searchTerms.all { searchReference.contains(it) }
                }
                .sortedBy { !it.isInstalled },
                key = App::packageName
            ) {
                AppEntry(it)
            }
        }
    }
}
