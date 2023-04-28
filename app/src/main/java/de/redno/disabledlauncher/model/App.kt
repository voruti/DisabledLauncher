package de.redno.disabledlauncher.model

import android.graphics.Bitmap
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class App(
    val name: String,
    val packageName: String,
    val isEnabled: Boolean,
    val isInstalled: Boolean,
    val icon: Bitmap,
    val overlyingListType: ListType? = null
) : Parcelable
