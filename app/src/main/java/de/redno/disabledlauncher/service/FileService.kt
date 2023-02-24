package de.redno.disabledlauncher.service

import android.content.Context
import android.net.Uri
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import java.io.IOException

object FileService {
    private val objectMapper = jacksonObjectMapper()

    fun <T> readFile(context: Context, uri: String, clazz: Class<T>): T? {
        try {
            // open & read file:
            return context.contentResolver.openInputStream(Uri.parse(uri))
                ?.use {
                    it.bufferedReader()
                        .use { it.readText() }
                }
                // deserialize into object:
                ?.let { return objectMapper.readValue(it, clazz) }
        } catch (e: IOException) {
            return null
        }
    }
}
