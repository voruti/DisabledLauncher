package de.redno.disabledlauncher.service

import android.content.Context
import de.redno.disabledlauncher.model.file.MainFile

object Datasource {

    fun loadAppList(context: Context): List<String> {
        context.getSharedPreferences(context.packageName, Context.MODE_PRIVATE)
            .getString("launchableAppsFile", null)?.let { uri ->

                FileService.readFile(context, uri, MainFile::class.java)
                    ?.let { return it.packages }
            }

        return emptyList()
    }
}
