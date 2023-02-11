package de.redno.disabledlauncher

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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
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
                    MainComponent()
                }
            }
        }
    }
}

@Composable
fun MainComponent(modifier: Modifier = Modifier) {
    AppList(appEntryList = Datasource().loadAppList())
}

@Composable
fun AppEntry(appEntryInList: AppEntryInList, modifier: Modifier = Modifier) {
    val context = LocalContext.current

    Box(
        modifier = Modifier.fillMaxWidth()
            .clickable {
                // show toast:
                Toast.makeText(context, appEntryInList.packageName, Toast.LENGTH_SHORT).show()
            }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(PaddingValues(horizontal = 16.dp, vertical = 8.dp))
        ) {
            // show app icon:
            Image(
                painter = painterResource(id = R.drawable.ic_launcher_background),
                contentDescription = "App Icon",
                modifier = Modifier
                    .size(48.dp)
                    .padding(PaddingValues(end = 16.dp))
            )
            Column {
                Text(
                    text = appEntryInList.name,
                    modifier = Modifier.padding(PaddingValues(bottom = 8.dp)),
                    style = MaterialTheme.typography.h6
                )
                Text(
                    text = appEntryInList.packageName,
//                modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.body2,
                    color = Color(0xFF808080)
                )
            }
        }
    }
}

@Composable
fun AppList(appEntryList: List<AppEntryInList>, modifier: Modifier = Modifier) {
    LazyColumn {
        items(items = appEntryList,
            key = { appEntry -> appEntry.packageName }
        ) { appEntry ->
            AppEntry(appEntry)
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun DefaultPreview() {
    DisabledLauncherTheme {
        MainComponent()
    }
}
