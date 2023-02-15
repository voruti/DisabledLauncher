package de.redno.disabledlauncher.data

import android.content.Context
import android.net.Uri
import org.json.JSONObject

class Datasource {

    fun loadAppList(context: Context): List<String> {
        context.getSharedPreferences("de.redno.disabledlauncher", Context.MODE_PRIVATE)
            .getString("launchableAppsFile", null)?.let { uri ->

                val inputStream = context.contentResolver.openInputStream(Uri.parse(uri))
                val jsonString = inputStream?.bufferedReader().use { it?.readText() }
                inputStream?.close()

                val jsonArray = jsonString?.let { JSONObject(it).getJSONArray("packages") }
                jsonArray?.let {
                    return (0 until jsonArray.length())
                        .map { jsonArray.getString(it) }
                }
            }

        return emptyList()
    }
}
