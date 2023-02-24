package de.redno.disabledlauncher

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity

class OpenAppActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // open app:
        if (intent.action == "${packageName}.action.OPEN_APP") {
            val packageNameToOpen = intent.getStringExtra("package_name")
            if (packageNameToOpen != null) {
                openAppLogic(this, getDetailsForPackage(this, packageNameToOpen))?.let {
                    error(it)
                }
            } else {
                error("Intent extras incorrect")
            }
        } else {
            error("Intent action incorrect")
        }

        // close again:
        finish()
    }

    private fun error(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        // switch to main activity:
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
    }
}
