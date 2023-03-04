package de.redno.disabledlauncher.service

import android.content.Context
import android.net.Uri
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import java.io.FileNotFoundException
import java.io.IOException

object FileService {
    private const val MAIN_FILE_NAME = "mainFile.json"

    private val objectMapper = jacksonObjectMapper()
        .enable(SerializationFeature.INDENT_OUTPUT)

    fun <T> readFile(context: Context, uri: String, clazz: Class<T>): T? {
        if (uri == Datasource.INTERNAL_MAIN_FILE) {
            touchInternalFile(context, MAIN_FILE_NAME, "{\"packages\":[]}")
        }

        try {
            // open & read file:
            val fileContent = if (uri == Datasource.INTERNAL_MAIN_FILE) {
                context.openFileInput(MAIN_FILE_NAME)
                    .bufferedReader().use {
                        it.readText()
                    }
            } else {
                context.contentResolver.openInputStream(Uri.parse(uri))?.use {
                    it.bufferedReader().use {
                        it.readText()
                    }
                }
            }
            return fileContent
                // deserialize into object:
                ?.let { return objectMapper.readValue(it, clazz) }
        } catch (e: IOException) {
            return null
        }
    }

    private fun touchInternalFile(context: Context, fileName: String, content: String) {
        try {
            context.openFileInput(fileName).close()
        } catch (_: FileNotFoundException) {
            // file doesn't exist, creating it:
            context.openFileOutput(fileName, Context.MODE_PRIVATE).use {
                it.write(content.toByteArray())
            }
        }
    }

    fun <T> writeFile(context: Context, uri: String, obj: T): Boolean {
        try {
            // serialize object:
            val jsonString = objectMapper.writeValueAsString(obj)

            // open & write file:
            if (uri == Datasource.INTERNAL_MAIN_FILE) {
                context.openFileOutput(MAIN_FILE_NAME, Context.MODE_PRIVATE).use {
                    it.write(jsonString.toByteArray())
                    return true
                }
            } else {
                context.contentResolver.openOutputStream(Uri.parse(uri), "wt")?.use {
                    it.bufferedWriter().use {
                        it.write(jsonString)
                        return true
                    }
                }
            }
        } catch (_: IOException) {
            // returning below
        }

        return false
    }
}
