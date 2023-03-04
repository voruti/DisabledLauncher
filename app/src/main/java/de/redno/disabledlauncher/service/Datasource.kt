package de.redno.disabledlauncher.service

import android.content.Context
import android.widget.Toast
import de.redno.disabledlauncher.R
import de.redno.disabledlauncher.asyncToastMakeText
import de.redno.disabledlauncher.model.file.MainFile
import kotlin.math.floor

object Datasource {
    const val INTERNAL_MAIN_FILE = "internalMainFile"

    fun loadAppList(context: Context): List<String> {
        getLaunchableAppsFileUri(context).let { uri ->

            FileService.readFile(context, uri, MainFile::class.java)
                ?.let { return it.packages }
        }

        asyncToastMakeText(context, context.getString(R.string.couldnt_load_app_list), Toast.LENGTH_SHORT)
        return emptyList()
    }

    fun raisePackage(context: Context, packageName: String): Boolean {
        getLaunchableAppsFileUri(context).let { uri ->

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
        getLaunchableAppsFileUri(context).let { uri ->

            FileService.readFile(context, uri, MainFile::class.java)?.let {
                // test for existance beforehand:
                if (!it.packages.contains(packageName)) {
                    return false
                }

                return FileService.writeFile(
                    context, uri, MainFile(
                        it.packages.filter { it != packageName })
                )
            }
        }

        return false
    }

    fun addPackages(context: Context, packageNameList: List<String>): Boolean {
        getLaunchableAppsFileUri(context).let { uri ->

            FileService.readFile(context, uri, MainFile::class.java)?.let {
                return FileService.writeFile(
                    context, uri, MainFile(
                        it.packages.toMutableList()
                            .also {
                                it.addAll(packageNameList)
                            }
                    )
                )
            }
        }

        return false
    }

    private fun getLaunchableAppsFileUri(context: Context): String {
        return context.getSharedPreferences(context.packageName, Context.MODE_PRIVATE)
            .getString("launchableAppsFile", INTERNAL_MAIN_FILE)!!
    }
}
