@file:Suppress("DEPRECATION")

package com.msu.mfalocker

import android.app.KeyguardManager
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Vibrator
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.msu.mfalocker.databinding.ActivityPinBinding
import java.io.File

class PinActivity : AppCompatActivity() {
    private lateinit var id: ActivityPinBinding
    private lateinit var vibrator: Vibrator

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        id = ActivityPinBinding.inflate(layoutInflater)
        vibrator = getSystemService(VIBRATOR_SERVICE) as Vibrator
        setContentView(id.root)

        val packageName = intent.getStringExtra("packageName") ?: ""
        val packages = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
        var exists = false

        for (packageInfo in packages) {
            if ((packageInfo.flags and ApplicationInfo.FLAG_SYSTEM) == 0) {
                if (packageInfo.packageName == packageName) {
                    id.icon.setImageDrawable(packageManager.getApplicationIcon(packageInfo))
                    id.appName.text = packageManager.getApplicationLabel(packageInfo).toString()
                    exists = true
                }
            }
        }

        if (!exists) {
            Toast.makeText(this, "App not found.", Toast.LENGTH_SHORT).show()
            goHome()
        }
        else {
            val keyGuard = getSystemService(KEYGUARD_SERVICE) as KeyguardManager
            if (keyGuard.isKeyguardSecure) {
                val intent = keyGuard.createConfirmDeviceCredentialIntent("Unlock ${id.appName.text}", "Touch fingerprint sensor or enter passcode.")
                if (intent != null) { startActivityForResult(intent, 1) }
                else {
                    Toast.makeText(applicationContext, "This device doesn't support app lock", Toast.LENGTH_SHORT).show()
                    goHome()
                }
            }
            else {
                Toast.makeText(applicationContext, "Setup screen lock first.", Toast.LENGTH_SHORT).show()
                goHome()
            }

            var passcode = ""
            val realPasscode = try { File(filesDir, "passcode.txt").readText() } catch (e: Exception) { "123456" }

            id.done.setOnClickListener {
                vibrator.vibrate(25)
                if (passcode == realPasscode) {
                    File(filesDir, "lastApp.txt").writeText(packageName)
                    finishAffinity()
                }
                else {
                    Toast.makeText(this, "Wrong passcode.", Toast.LENGTH_SHORT).show()
                    passcode = ""
                    clearPasscode()
                }
            }

            id.one.setOnClickListener {
                if (passcode.length < 6) {
                    passcode += "1"
                    updatePasscode(passcode.length)
                }
            }

            id.two.setOnClickListener {
                if (passcode.length < 6) {
                    passcode += "2"
                    updatePasscode(passcode.length)
                }
            }

            id.three.setOnClickListener {
                if (passcode.length < 6) {
                    passcode += "3"
                    updatePasscode(passcode.length)
                }
            }

            id.four.setOnClickListener {
                if (passcode.length < 6) {
                    passcode += "4"
                    updatePasscode(passcode.length)
                }
            }

            id.five.setOnClickListener {
                if (passcode.length < 6) {
                    passcode += "5"
                    updatePasscode(passcode.length)
                }
            }

            id.six.setOnClickListener {
                if (passcode.length < 6) {
                    passcode += "6"
                    updatePasscode(passcode.length)
                }
            }

            id.seven.setOnClickListener {
                if (passcode.length < 6) {
                    passcode += "7"
                    updatePasscode(passcode.length)
                }
            }

            id.eight.setOnClickListener {
                if (passcode.length < 6) {
                    passcode += "8"
                    updatePasscode(passcode.length)
                }
            }

            id.nine.setOnClickListener {
                if (passcode.length < 6) {
                    passcode += "9"
                    updatePasscode(passcode.length)
                }
            }

            id.zero.setOnClickListener {
                if (passcode.length < 6) {
                    passcode += "0"
                    updatePasscode(passcode.length)
                }
            }
        }
    }

    private fun clearPasscode() {
        id.pass1.visibility = View.GONE
        id.pass2.visibility = View.GONE
        id.pass3.visibility = View.GONE
        id.pass4.visibility = View.GONE
        id.pass5.visibility = View.GONE
        id.pass6.visibility = View.GONE
    }

    private fun goHome() {
        val intent = Intent(Intent.ACTION_MAIN)
        intent.addCategory(Intent.CATEGORY_HOME)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
        finishAffinity()
    }

    private fun updatePasscode(length: Int) {
        vibrator.vibrate(25)
        when (length) {
            1 -> id.pass1.visibility = View.VISIBLE
            2 -> id.pass2.visibility = View.VISIBLE
            3 -> id.pass3.visibility = View.VISIBLE
            4 -> id.pass4.visibility = View.VISIBLE
            5 -> id.pass5.visibility = View.VISIBLE
            6 -> id.pass6.visibility = View.VISIBLE
        }
    }

    @Deprecated("This method has been deprecated in favor of using the\n      {@link OnBackPressedDispatcher} via {@link #getOnBackPressedDispatcher()}.\n      The OnBackPressedDispatcher controls how back button events are dispatched\n      to one or more {@link OnBackPressedCallback} objects.",
        ReplaceWith("super.onBackPressed()", "androidx.appcompat.app.AppCompatActivity")
    )
    override fun onBackPressed() {
        if (id.textView24.text == id.textView23.text) super.onBackPressed()
        else goHome()
    }

    @Deprecated("This method has been deprecated in favor of using the Activity Result API\n      which brings increased type safety via an {@link ActivityResultContract} and the prebuilt\n      contracts for common intents available in\n      {@link androidx.activity.result.contract.ActivityResultContracts}, provides hooks for\n      testing, and allow receiving results in separate, testable classes independent from your\n      activity. Use\n      {@link #registerForActivityResult(ActivityResultContract, ActivityResultCallback)}\n      with the appropriate {@link ActivityResultContract} and handling the result in the\n      {@link ActivityResultCallback#onActivityResult(Object) callback}.")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1) {
            if (resultCode != RESULT_OK) {
                Toast.makeText(this, "Authentication failed.", Toast.LENGTH_SHORT).show()
                goHome()
            }
            else {
                if (File(filesDir, "passcode.txt").exists()) {
                    Toast.makeText(this, "Enter passcode.", Toast.LENGTH_SHORT).show()
                    id.locker.visibility = View.VISIBLE
                    id.locker.startAnimation(android.view.animation.AnimationUtils.loadAnimation(this, android.R.anim.fade_in))
                }
                else {
                    File(filesDir, "lastApp.txt").writeText(intent.getStringExtra("packageName") ?: "")
                    finishAffinity()
                }
            }
        }
    }
}