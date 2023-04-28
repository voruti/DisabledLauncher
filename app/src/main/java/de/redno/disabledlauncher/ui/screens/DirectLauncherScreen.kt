package de.redno.disabledlauncher.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.padding
import androidx.compose.material.FloatingActionButton
import androidx.compose.material.Icon
import androidx.compose.material.Scaffold
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AppBlocking
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import de.redno.disabledlauncher.R
import de.redno.disabledlauncher.common.AndroidUtil
import de.redno.disabledlauncher.model.ListType
import de.redno.disabledlauncher.model.exception.DisabledLauncherException
import de.redno.disabledlauncher.service.AppService
import de.redno.disabledlauncher.ui.components.AppList
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
