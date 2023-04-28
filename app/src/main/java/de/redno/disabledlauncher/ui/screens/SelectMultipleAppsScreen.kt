package de.redno.disabledlauncher.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.padding
import androidx.compose.material.FloatingActionButton
import androidx.compose.material.Icon
import androidx.compose.material.Scaffold
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.graphics.drawable.toBitmap
import de.redno.disabledlauncher.R
import de.redno.disabledlauncher.common.AndroidUtil
import de.redno.disabledlauncher.model.App
import de.redno.disabledlauncher.ui.components.AppList
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
        AppList(
            appList = selectableApps,
            selectedAppList = selectedAppList,
            modifier = Modifier.padding(it)
        )
    }
}
