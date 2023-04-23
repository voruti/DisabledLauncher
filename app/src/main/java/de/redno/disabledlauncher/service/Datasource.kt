package de.redno.disabledlauncher.service

import android.content.Context
import android.widget.Toast
import de.redno.disabledlauncher.R
import de.redno.disabledlauncher.common.AndroidUtil
import de.redno.disabledlauncher.model.file.MainFile
import kotlin.math.floor

object Datasource {
    const val INTERNAL_MAIN_FILE = "internalMainFile"

    fun loadAppList(context: Context, listType: ListType = ListType.MAIN): List<String> {
        getLaunchableAppsFileUri(context).let {

            FileService.readFile(context, it, MainFile::class.java)
                ?.let {
                    return if (listType == ListType.MAIN) {
                        it.packages
                    } else {
                        it.longTermPackages ?: emptyList()
                    }
                }
        }

        AndroidUtil.asyncToastMakeText(context, context.getString(R.string.couldnt_load_app_list), Toast.LENGTH_SHORT)
        return emptyList()
    }

    fun raisePackage(context: Context, packageName: String, listType: ListType = ListType.MAIN): Boolean {
        getLaunchableAppsFileUri(context).let { uri ->

            FileService.readFile(context, uri, MainFile::class.java)?.let {
                var packages = it.packages
                var longTermPackages = it.longTermPackages ?: emptyList()

                val oldIndex = (if (listType == ListType.MAIN) packages else longTermPackages)
                    .indexOf(packageName)
                if (oldIndex <= 0) {
                    return false
                }

                fun moveToIndex(list: List<String>): List<String> {
                    return list
                        .filter { it != packageName }
                        .toMutableList()
                        .also {
                            it.add(
                                floor((oldIndex * 2) / 3f).toInt(),
                                packageName
                            )
                        }
                }

                if (listType == ListType.MAIN) {
                    packages = moveToIndex(packages)
                } else {
                    longTermPackages = moveToIndex(longTermPackages)
                }

                return FileService.writeFile(
                    context, uri, MainFile(packages, longTermPackages)
                )
            }
        }

        return false
    }

    fun removePackage(context: Context, packageName: String, listType: ListType = ListType.MAIN): Boolean {
        getLaunchableAppsFileUri(context).let { uri ->

            FileService.readFile(context, uri, MainFile::class.java)?.let {
                var packages = it.packages
                var longTermPackages = it.longTermPackages ?: emptyList()

                // test for existence beforehand:
                if (!
                    (if (listType == ListType.MAIN) packages else longTermPackages)
                        .contains(packageName)
                ) {
                    return false
                }

                // remove:
                if (listType == ListType.MAIN) {
                    packages = packages.filter { it != packageName }
                } else {
                    longTermPackages = longTermPackages.filter { it != packageName }
                }

                return FileService.writeFile(
                    context, uri, MainFile(packages, longTermPackages)
                )
            }
        }

        return false
    }

    fun addPackages(context: Context, packageNameList: List<String>, listType: ListType = ListType.MAIN): Boolean {
        getLaunchableAppsFileUri(context).let { uri ->

            FileService.readFile(context, uri, MainFile::class.java)?.let {
                val packages = it.packages.toMutableList()
                val longTermPackages = (it.longTermPackages ?: emptyList()).toMutableList()

                (if (listType == ListType.MAIN) packages else longTermPackages)
                    .also {
                        it.addAll(packageNameList)
                    }

                val success = FileService.writeFile(
                    context, uri, MainFile(packages, longTermPackages)
                )

                if (success) {
                    AndroidUtil.asyncToastMakeText(
                        context,
                        String.format(context.getString(R.string.success_added_x_apps), packageNameList.size),
                        Toast.LENGTH_SHORT
                    )
                }

                return success
            }
        }

        return false
    }

    private fun getLaunchableAppsFileUri(context: Context): String {
        return context.getSharedPreferences(context.packageName, Context.MODE_PRIVATE)
            .getString("launchableAppsFile", INTERNAL_MAIN_FILE)!!
    }

    enum class ListType {
        MAIN,
        LONG_TERM
    }
}
