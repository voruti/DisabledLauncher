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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.google.accompanist.drawablepainter.rememberDrawablePainter
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
    val appEntry = AppEntryInList(
        name = "App not found",
        packageName = packageName,
        icon = context.getDrawable(R.drawable.ic_launcher_background),
        isEnabled = false
    )

    try {
        context.packageManager.getPackageInfo(packageName, 0)?.let { packageInfo ->
            appEntry.name = packageInfo.applicationInfo.loadLabel(context.packageManager).toString()
            appEntry.packageName = packageInfo.packageName
            appEntry.icon = packageInfo.applicationInfo.loadIcon(context.packageManager)
            appEntry.isEnabled = packageInfo.applicationInfo.enabled
        }
    } catch (e: PackageManager.NameNotFoundException) {
        e.printStackTrace()
    }

    return appEntry
}


@Composable
fun MainComponent(installedApps: List<String>, modifier: Modifier = Modifier) {
    AppList(appEntryList = installedApps)
}

@Composable
fun AppEntry(packageName: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val appEntry = getDetailsForPackage(context, packageName)

    Box(
        modifier = Modifier.fillMaxWidth()
            .clickable {
                // show toast:
                Toast.makeText(context, packageName, Toast.LENGTH_SHORT).show()
            }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(PaddingValues(horizontal = 24.dp, vertical = 8.dp))
        ) {
            // show app icon:
            Image(
                painter = rememberDrawablePainter(appEntry.icon),
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
                    text = packageName,
                    maxLines = 2, // maybe different design/layout
                    style = MaterialTheme.typography.body2,
                    color = Color(0xFF808080)
                )
            }
        }
    }
}

@Composable
fun AppList(appEntryList: List<String>, modifier: Modifier = Modifier) {
    LazyColumn {
        items(items = appEntryList,
            key = { appEntry -> appEntry }
        ) { appEntry ->
            AppEntry(appEntry)
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
