package de.redno.disabledlauncher.model.file

data class MainFile(
    val packages: List<String>,
    // TODO: other & additional configuration in this file? -> what belongs here vs. what in shared prefs?
    // TODO: device specific configuration?
    val longTermPackages: List<String>?
)
