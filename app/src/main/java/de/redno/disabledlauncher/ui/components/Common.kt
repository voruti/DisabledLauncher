package de.redno.disabledlauncher.ui.components

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import de.redno.disabledlauncher.ActionReceiverActivity
import de.redno.disabledlauncher.MainActivity
import de.redno.disabledlauncher.R
import de.redno.disabledlauncher.common.AndroidUtil
import de.redno.disabledlauncher.model.App
import de.redno.disabledlauncher.model.exception.DisabledLauncherException
import de.redno.disabledlauncher.service.AppService
import de.redno.disabledlauncher.service.Datasource
import de.redno.disabledlauncher.ui.theme.DisabledLauncherTheme

fun clickableIcon(
    imageVector: ImageVector,
    contentDescription: String,
    onClick: () -> Unit
): @Composable () -> Unit {
    return {
        IconButton(onClick = onClick) {
            Icon(imageVector, contentDescription)
        }
    }
}


@Preview(showBackground = true, showSystemUi = true)
@Composable
fun DefaultPreview() {
    DisabledLauncherTheme {
        Column {
            ToolbarComponent(title = "Preview", onBackNavigation = {}, onSettingsClick = {})

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
    onMenuClick: (() -> Unit)? = null,
    onBackNavigation: (() -> Unit)? = null,
    onSettingsClick: (() -> Unit)? = null
) {
    TopAppBar(
        modifier = modifier,
        navigationIcon = onMenuClick?.let {
            clickableIcon(Icons.Default.Menu, stringResource(id = R.string.menu_icon), it)
        } ?: onBackNavigation?.let {
            clickableIcon(Icons.Default.ArrowBack, stringResource(id = R.string.back_icon), it)
        },
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppEntry(
    app: App,
    modifier: Modifier = Modifier,
    selectedAppList: SnapshotStateList<App>? = null
) {
    val context = LocalContext.current
    val sharedPreferences = context.getSharedPreferences(context.packageName, Context.MODE_PRIVATE)

    var dropdownExpanded by remember { mutableStateOf(false) }
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
        startContent = selectedAppList?.let {
            {
                Checkbox(checked = it.any { it.packageName == app.packageName }, onCheckedChange = null)
            }
        },
        contextContent = {
            DropdownMenu(
                expanded = dropdownExpanded,
                onDismissRequest = { dropdownExpanded = false }
            ) {
                DropdownMenuItem(onClick = {
                    if (ShortcutManagerCompat.isRequestPinShortcutSupported(context)) {
                        val intent = Intent(context, ActionReceiverActivity::class.java)
                            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            .setAction("${context.packageName}.action.OPEN_APP")
                            .putExtra("package_name", app.packageName)
                        val shortcutInfo = ShortcutInfoCompat.Builder(context, app.packageName)
                            .setShortLabel(app.name)
                            .setLongLabel(app.name)
                            .setIcon(IconCompat.createWithBitmap(app.icon.asImageBitmap().asAndroidBitmap()))
                            .setIntent(intent)
                            .build()
                        ShortcutManagerCompat.requestPinShortcut(context, shortcutInfo, null)
                    } else {
                        Toast.makeText(
                            context,
                            context.getString(R.string.launcher_not_support_pinned),
                            Toast.LENGTH_LONG
                        ).show()
                    }

                    dropdownExpanded = false
                }) {
                    Text(stringResource(R.string.add_shortcut))
                }
                app.overlyingListType?.let {
                    DropdownMenuItem(onClick = {
                        if (Datasource.removePackage(context, app.packageName, it)) {
                            dropdownExpanded = false

                            AndroidUtil.asyncToastMakeText(
                                context,
                                String.format(context.getString(R.string.app_removed), app.name),
                                Toast.LENGTH_SHORT
                            )
                            // TODO: refresh app list (+ there are other actions/code locations that need to trigger a list refresh)
                        } else {
                            Toast.makeText(context, context.getString(R.string.couldnt_remove_app), Toast.LENGTH_LONG)
                                .show()
                        }
                    }) {
                        Text(stringResource(R.string.remove_app))
                    }
                }
            }
        },
        modifier = modifier.combinedClickable(
            onClick = {
                Thread {
                    if (app.isInstalled) {
                        try {
                            AppService.openAppLogic(context, app)
                            if (sharedPreferences.getBoolean("sortAppsByUsage", false)) {
                                Datasource.raisePackage(context, app.packageName)
                            }
                            MainActivity.lastObject?.finishAndRemoveTask() // TODO: configurable setting to keep app open
                        } catch (e: DisabledLauncherException) {
                            e.getLocalizedMessage(context)?.let {
                                AndroidUtil.asyncToastMakeText(context, it, Toast.LENGTH_SHORT)
                            }
                        }
                    }
                }.start()
            },
            onLongClick = { dropdownExpanded = true }
        ).then(
            selectedAppList?.let {
                Modifier.toggleable( // overrides combinedClickable: -> no open app nor dropdown
                    role = Role.Checkbox,
                    value = selectedAppList.any { it.packageName == app.packageName },
                    onValueChange = {
                        if (it) {
                            selectedAppList.add(app)
                        } else {
                            selectedAppList.removeIf { it.packageName == app.packageName }
                        }
                    }
                )
            } ?: Modifier
        )
    )
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
