package de.redno.disabledlauncher.common

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.widget.Toast

object AndroidUtil {
    fun asyncToastMakeText(context: Context, text: CharSequence, duration: Int) { // TODO: move every "global" function
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(context, text, duration).show()
        }
    }
}
