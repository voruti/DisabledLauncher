package de.redno.disabledlauncher.model.exception

import android.content.res.Resources
import de.redno.disabledlauncher.R

class ShizukuUnavailableException : ShizukuException(Resources.getSystem().getString(R.string.shizuku_unavailable))
