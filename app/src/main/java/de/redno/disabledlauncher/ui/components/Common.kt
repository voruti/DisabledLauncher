package de.redno.disabledlauncher.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import de.redno.disabledlauncher.R
import de.redno.disabledlauncher.ui.theme.DisabledLauncherTheme

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun DefaultPreview() {
    DisabledLauncherTheme {
        Column {
            ToolbarComponent(title = "Preview", onSettingsClick = {})

            var test by remember { mutableStateOf(true) }
            ListItem(
                title = "Title",
                description = "Desc",
                startContent = {
                    Checkbox(checked = false, onCheckedChange = null)
                },
                endContent = {
                    Switch(checked = test, onCheckedChange = null)
                }
            )
        }
    }
}


@Composable
fun ToolbarComponent(
    title: String,
    modifier: Modifier = Modifier,
    onSettingsClick: (() -> Unit)? = null
) {
    TopAppBar(
        modifier = modifier,
        title = { Text(text = title) },
        actions = {
            onSettingsClick?.let {
                IconButton(onClick = onSettingsClick) {
                    Icon(Icons.Default.Settings, contentDescription = stringResource(id = R.string.settings))
                }
            }
        }
    )
}

@Composable
fun ListItem(
    title: String,
    description: String,
    modifier: Modifier = Modifier,
    icon: @Composable BoxScope.() -> Unit = {},
    startContent: (@Composable (() -> Unit))? = null,
    endContent: (@Composable (() -> Unit))? = null,
    contextContent: (@Composable (() -> Unit))? = null,
    italicStyle: Boolean = false,
    disabledStyle: Boolean = false,
) {
    val boxModifier = if (disabledStyle)
        modifier.background(MaterialTheme.colors.onSurface.copy(alpha = 0.12f)) else
        modifier
    Box(
        modifier = boxModifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(PaddingValues(horizontal = 24.dp, vertical = 8.dp))
        ) {
            startContent?.let {
                Box(modifier = Modifier.padding(PaddingValues(end = 16.dp))) {
                    startContent()
                }
            }
            Box(
                modifier = Modifier.size(64.dp)
                    .padding(PaddingValues(end = 16.dp)),
                contentAlignment = Alignment.Center,
                content = icon
            )
            Column(modifier = Modifier.weight(1F)) {
                val titleStyle = MaterialTheme.typography.h6.merge(
                    if (disabledStyle)
                        TextStyle(textDecoration = TextDecoration.LineThrough) else
                        TextStyle()
                )
                Text(
                    text = title,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(PaddingValues(bottom = 4.dp)),
                    style = titleStyle,
                    fontStyle = if (italicStyle) FontStyle.Italic else FontStyle.Normal
                )
                Text(
                    text = description,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.body2,
                    color = Color(0xFF808080)
                )
            }
            endContent?.let {
                Box(modifier = Modifier.padding(PaddingValues(start = 16.dp))) {
                    endContent()
                }
            }
        }
        contextContent?.let {
            contextContent()
        }
    }
}

@Composable
fun ConditionalDialog(
    showDialog: MutableState<Boolean>,
    content: @Composable () -> Unit
) {
    if (showDialog.value) { // TODO: loading animation
        Dialog(
            onDismissRequest = { showDialog.value = false },
            content = content
        )
    }
}
