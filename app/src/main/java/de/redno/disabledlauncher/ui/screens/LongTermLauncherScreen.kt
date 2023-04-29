package de.redno.disabledlauncher.ui.screens

import androidx.compose.foundation.layout.padding
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import de.redno.disabledlauncher.R
import de.redno.disabledlauncher.model.App
import de.redno.disabledlauncher.service.AppService
import de.redno.disabledlauncher.ui.components.AppList
import de.redno.disabledlauncher.ui.components.ToolbarComponent
import de.redno.disabledlauncher.ui.theme.DisabledLauncherTheme

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun LongTermLauncherPreview() {
    val context = LocalContext.current

    DisabledLauncherTheme {
        LongTermLauncherScreen(
            onMenuClick = {},
            appList = listOf("de.test.1", "de.test.2", "de.test.3").map {
                AppService.getDetailsForPackage(context, it)
            }
        )
    }
}


@Composable
fun LongTermLauncherScreen(
    onMenuClick: () -> Unit,
    appList: List<App>,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val selectedAppList = remember { mutableStateListOf<App>() }
    selectedAppList.addAll(appList.filter { it.isEnabled })
    Scaffold(
        modifier = modifier,
        topBar = {
            ToolbarComponent(
                title = stringResource(id = R.string.long_term_launcher_title),
                onMenuClick = onMenuClick
            )
        }
    ) {
        AppList(
            appList = appList,
            selectedAppList = selectedAppList,
            onSelectedValueChangeAsWell = { app, checked ->
                if (checked) {
                    AppService.openAppLogic(context, app)
                    // TODO: alternatively only enable app, configurable in settings
                } else {
                    AppService.disableApp(context, app, true)
                }
            },
            modifier = Modifier.padding(it)
        )
    }
}
