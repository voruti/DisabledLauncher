package de.redno.disabledlauncher.model.exception

import android.content.res.Resources
import de.redno.disabledlauncher.R

class NoShizukuPermissionException : ShizukuException(Resources.getSystem().getString(R.string.shizuku_denied))
