package de.redno.disabledlauncher

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import de.redno.disabledlauncher.model.exception.DisabledLauncherException
import de.redno.disabledlauncher.model.exception.RedirectedToGooglePlayException

class ActionReceiverActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        when (intent.action) {
            "${packageName}.action.OPEN_APP" -> {
                val packageNameToOpen = intent.getStringExtra("package_name")
                if (packageNameToOpen != null && packageNameToOpen.matches(Regex("^\\w+\\.[\\w.]*\\w+$"))) {
                    try {
                        openAppLogic(this, getDetailsForPackage(this, packageNameToOpen))
                    } catch (e: DisabledLauncherException) {
                        e.getLocalizedMessage(this)?.let {
                            error(it, e !is RedirectedToGooglePlayException)
                        }
                    }
                } else {
                    error(getString(R.string.intent_extras_incorrect))
                }
            }

            "${packageName}.action.DISABLE_ALL_APPS" -> {
                try {
                    disableAllApps(this)
                } catch (e: DisabledLauncherException) {
                    e.getLocalizedMessage(this)?.let {
                        error(it)
                    }
                }
            }

            else -> error(getString(R.string.intent_action_incorrect))
        }

        // close again:
        finish()
    }

    private fun error(message: String, openMainActivity: Boolean = true) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()

        // switch to main activity:
        if (openMainActivity) {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }
    }
}
