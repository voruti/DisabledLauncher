package de.redno.disabledlauncher

import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import de.redno.disabledlauncher.data.Datasource
import de.redno.disabledlauncher.model.AppEntryInList
import de.redno.disabledlauncher.ui.theme.DisabledLauncherTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DisabledLauncherTheme {
                // A surface container using the 'background' color from the theme
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colors.background) {
                    MainComponent(Datasource().loadAppList())
                }
            }
        }
    }
}


fun getDetailsForPackage(context: Context, packageName: String): AppEntryInList {
    val packageManager = context.packageManager

    try {
        packageManager.getPackageInfo(packageName, 0)?.let { packageInfo ->
            return AppEntryInList(
                packageInfo.applicationInfo.loadLabel(packageManager).toString(),
                packageInfo.packageName,
                packageInfo.applicationInfo.enabled,
                packageInfo.applicationInfo.loadIcon(packageManager).toBitmap()
            )
        }
    } catch (e: PackageManager.NameNotFoundException) {
        e.printStackTrace()
    }

    return AppEntryInList(
        name = "App not found",
        packageName = packageName,
        icon = context.getDrawable(R.drawable.ic_launcher_background)!!.toBitmap(),
        isEnabled = false
    )
}


@Composable
fun MainComponent(packageNameList: List<String>, modifier: Modifier = Modifier) {
    AppList(packageNameList = packageNameList)
}

@Composable
fun AppEntry(appEntry: AppEntryInList, modifier: Modifier = Modifier) {
    val context = LocalContext.current

    Box(
        modifier = Modifier.fillMaxWidth()
            .clickable {
                // show toast:
                Toast.makeText(context, appEntry.packageName, Toast.LENGTH_SHORT).show()
            }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(PaddingValues(horizontal = 24.dp, vertical = 8.dp))
        ) {
            // show app icon:
            Image(
                bitmap = appEntry.icon.asImageBitmap(),
                contentDescription = "App Icon",
                modifier = Modifier
                    .size(64.dp)
                    .padding(PaddingValues(end = 16.dp))
            )
            Column {
                Text(
                    text = appEntry.name,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(PaddingValues(bottom = 4.dp)),
                    style = MaterialTheme.typography.h6
                )
                Text(
                    text = appEntry.packageName,
                    maxLines = 2, // maybe different design/layout
                    style = MaterialTheme.typography.body2,
                    color = Color(0xFF808080)
                )
            }
        }
    }
}

@Composable
fun AppList(packageNameList: List<String>, modifier: Modifier = Modifier) {
    val context = LocalContext.current

    var text by remember { mutableStateOf("") }
    val appEntryList =
        packageNameList.map { getDetailsForPackage(context, it) } // TODO: prevent being called on every text change

    Column {
        TextField( // TODO: https://developer.android.com/jetpack/compose/text#enter-modify-text
            value = text,
            onValueChange = { text = it },
            placeholder = { Text("Search for apps...") },
            modifier = Modifier.fillMaxWidth()
        )
        LazyColumn {
            items(items = appEntryList.filter {
                val searchTerms = text.trim().lowercase().split(" ")
                val searchReference = "${it.name.trim().lowercase()} ${it.packageName.trim().lowercase()}"

                searchTerms.all { searchTerm -> searchReference.contains(searchTerm) }
            },
                key = { it }
            ) { AppEntry(it) }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun DefaultPreview() {
    DisabledLauncherTheme {
        MainComponent(Datasource().loadAppList())
    }
}
