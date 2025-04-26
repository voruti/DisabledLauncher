package de.redno.disabledlauncher.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AppBlocking
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import de.redno.disabledlauncher.R
import de.redno.disabledlauncher.common.AndroidUtil
import de.redno.disabledlauncher.model.App
import de.redno.disabledlauncher.model.exception.DisabledLauncherException
import de.redno.disabledlauncher.service.AppService
import de.redno.disabledlauncher.ui.components.AppList
import de.redno.disabledlauncher.ui.components.ToolbarComponent
import de.redno.disabledlauncher.ui.theme.DisabledLauncherTheme

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun DirectLauncherPreview() {
    val context = LocalContext.current

    DisabledLauncherTheme {
        DirectLauncherScreen(
            onMenuClick = {},
            appList = listOf("de.test.1", "de.test.2", "de.test.3").map {
                AppService.getDetailsForPackage(context, it)
            }
        )
    }
}


@Composable
fun DirectLauncherScreen(
    onMenuClick: () -> Unit,
    appList: List<App>,
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
            FloatingActionButton(modifier = Modifier.safeDrawingPadding(), onClick = Thread {
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
            appList = appList,
            modifier = Modifier.padding(it)
        )
    }
}
