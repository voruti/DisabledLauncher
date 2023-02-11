package de.redno.disabledlauncher.model

import android.graphics.drawable.Drawable

data class AppEntryInList(
    var name: String,
    var packageName: String,
    var isEnabled: Boolean,
    var icon: Drawable?
)
