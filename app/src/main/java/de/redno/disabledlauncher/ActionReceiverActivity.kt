package de.redno.disabledlauncher

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import de.redno.disabledlauncher.model.exception.DisabledLauncherException
import de.redno.disabledlauncher.model.exception.RedirectedToGooglePlayException
import de.redno.disabledlauncher.service.AppService

class ActionReceiverActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        when (intent.action) {
            "${packageName}.action.OPEN_APP" -> {
                val packageNameToOpen = intent.getStringExtra("package_name")
                if (packageNameToOpen != null && packageNameToOpen.matches(Regex("^\\w+\\.[\\w.]*\\w+$"))) {
                    val app = AppService.getDetailsForPackage(this, packageNameToOpen)
                    if (app.isInstalled) {
                        try {
                            AppService.openAppLogic(this, app)
                            setResult(RESULT_OK, Intent())
                        } catch (e: DisabledLauncherException) {
                            e.getLocalizedMessage(this)?.let {
                                error(it, e !is RedirectedToGooglePlayException)
                            }
                        }
                    } else {
                        error(getString(R.string.app_not_found), true)
                    }
                } else {
                    error(getString(R.string.intent_extras_incorrect))
                }
            }

            "${packageName}.action.DISABLE_ALL_APPS" -> {
                try {
                    AppService.disableAllApps(this)
                    setResult(RESULT_OK, Intent())
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

    private fun error(message: String, openMainActivity: Boolean = false) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()

        // switch to main activity:
        if (openMainActivity) {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }
    }
}
