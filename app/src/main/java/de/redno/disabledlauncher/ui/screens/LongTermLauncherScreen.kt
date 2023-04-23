package de.redno.disabledlauncher.ui.screens

import androidx.compose.foundation.layout.padding
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import de.redno.disabledlauncher.R
import de.redno.disabledlauncher.model.ListType
import de.redno.disabledlauncher.ui.components.ToolbarComponent
import de.redno.disabledlauncher.ui.theme.DisabledLauncherTheme

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun LongTermLauncherPreview() {
    DisabledLauncherTheme {
        LongTermLauncherScreen(
            onMenuClick = {},
            packageNameList = listOf("de.test.1", "de.test.2", "de.test.3")
        )
    }
}


@Composable
fun LongTermLauncherScreen(
    onMenuClick: () -> Unit,
    packageNameList: List<String>,
    modifier: Modifier = Modifier
) {
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
            packageNameList = packageNameList,
            listType = ListType.LONG_TERM,
            modifier = Modifier.padding(it)
        )
    }
}
