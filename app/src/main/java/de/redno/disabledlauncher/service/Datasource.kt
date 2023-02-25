package de.redno.disabledlauncher.service

import android.content.Context
import de.redno.disabledlauncher.model.file.MainFile
import kotlin.math.floor

object Datasource {

    fun loadAppList(context: Context): List<String> {
        context.getSharedPreferences(context.packageName, Context.MODE_PRIVATE)
            .getString("launchableAppsFile", null)?.let { uri ->

                FileService.readFile(context, uri, MainFile::class.java)
                    ?.let { return it.packages }
            }

        return emptyList()
    }

    fun raisePackage(context: Context, packageName: String): Boolean {
        context.getSharedPreferences(context.packageName, Context.MODE_PRIVATE)
            .getString("launchableAppsFile", null)?.let { uri ->

                FileService.readFile(context, uri, MainFile::class.java)?.let {
                    val oldIndex = it.packages.indexOf(packageName)
                    if (oldIndex <= 0) {
                        return false
                    }

                    return FileService.writeFile(context, uri, MainFile(
                        it.packages
                            .filter { it != packageName }
                            .toMutableList()
                            .also {
                                it.add(
                                    floor((oldIndex * 2) / 3f).toInt(),
                                    packageName
                                )
                            }
                    ))
                }
            }

        return false
    }

    fun removePackage(context: Context, packageName: String): Boolean {
        context.getSharedPreferences(context.packageName, Context.MODE_PRIVATE)
            .getString("launchableAppsFile", null)?.let { uri ->

                FileService.readFile(context, uri, MainFile::class.java)?.let {
                    return FileService.writeFile(
                        context, uri, MainFile(
                            it.packages.filter { it != packageName })
                    )
                }
            }

        return false
    }
}
