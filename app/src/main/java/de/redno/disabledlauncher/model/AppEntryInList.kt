package de.redno.disabledlauncher.model

data class AppEntryInList(
    val name: String,
    val packageName: String,
    val icon: String,
    val isDisabled: Boolean
)
