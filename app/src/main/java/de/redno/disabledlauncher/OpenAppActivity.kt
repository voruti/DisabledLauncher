package de.redno.disabledlauncher

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity

class OpenAppActivity : ComponentActivity() { // TODO: open activity invisible or something?
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        var error = "Intent action incorrect"
        if (intent.action == "${packageName}.action.OPEN_APP") {
            val packageNameToOpen = intent.getStringExtra("package_name")
            error = if (packageNameToOpen == null) {
                "Intent extras incorrect"
            } else {
                val errorMessage = openAppLogic(this, getDetailsForPackage(this, packageNameToOpen))
                if (errorMessage != null) {
                    errorMessage
                } else {
                    // close self after successful opening:
                    finish()
                    return
                }
            }
        }

        // show error:
        Toast.makeText(this, error, Toast.LENGTH_LONG).show()
        // switch to main activity:
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }
}
