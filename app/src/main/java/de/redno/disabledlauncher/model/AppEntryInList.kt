package de.redno.disabledlauncher.model

import android.graphics.drawable.Drawable

data class AppEntryInList(
    val name: String,
    val packageName: String,
    val isEnabled: Boolean,
    val icon: Drawable?
)
