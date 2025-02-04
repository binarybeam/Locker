@file:Suppress("DEPRECATION")

package com.msu.mfalocker

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.app.AppOpsManager
import android.app.KeyguardManager
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.LinearLayoutManager
import com.msu.mfalocker.databinding.ActivityMainBinding
import eightbitlab.com.blurview.RenderScriptBlur
import java.io.File

class MainActivity : AppCompatActivity() {
    private lateinit var id: ActivityMainBinding
    private lateinit var vibrator: Vibrator
    private var appsList = ArrayList<App>()
    private lateinit var lockedAppsList: ArrayList<String>
    private lateinit var lockedFile: File
    private lateinit var ifLockedFile: File
    private var pausedFor = 0
    private var type = ""

    @SuppressLint("HardwareIds")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        id = ActivityMainBinding.inflate(layoutInflater)
        setContentView(id.root)

        lockedFile = File(filesDir, "locked.txt")
        ifLockedFile = File(filesDir, "ifLocked.txt")
        lockedAppsList = if (lockedFile.exists() && lockedFile.readText().isNotEmpty()) ArrayList(File(filesDir, "locked.txt").readText().split(",")) else ArrayList()

        if (ifLockedFile.exists() && ifLockedFile.readText() == "true") {
            if (!Settings.canDrawOverlays(this) || !isUsagePermitted()) {
                Toast.makeText(this, "Permission not granted.", Toast.LENGTH_SHORT).show()

                type = "none"
                id.materialSwitch.isChecked = false
                id.appsList.alpha = 0.5f
            }
            else {
                type = ""
                id.materialSwitch.isChecked = true
                id.appsList.alpha = 1f
            }
        }
        else {
            type = "none"
            id.materialSwitch.isChecked = false
            id.appsList.alpha = 0.5f
        }

        val keyGuard = getSystemService(KEYGUARD_SERVICE) as KeyguardManager
        id.locker.setupWith(findViewById(android.R.id.content), RenderScriptBlur(this)).setBlurRadius(25f)

        if (keyGuard.isKeyguardSecure) {
            val intent = keyGuard.createConfirmDeviceCredentialIntent("Unlock ${getString(R.string.app_name)}", "Touch fingerprint sensor or enter passcode.")
            if (intent != null) { startActivityForResult(intent, 1) }
            else {
                Toast.makeText(applicationContext, "This device doesn't support app lock", Toast.LENGTH_SHORT).show()
                finishAffinity()
            }
        }
        else {
            Toast.makeText(applicationContext, "Setup screen lock first.", Toast.LENGTH_SHORT).show()
            finishAffinity()
        }

        val packages = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
        for (packageInfo in packages) {
            if ((packageInfo.flags and ApplicationInfo.FLAG_SYSTEM) == 0) {
                val appName = packageManager.getApplicationLabel(packageInfo).toString()
                val pkgName = packageInfo.packageName
                val appIcon = packageManager.getApplicationIcon(packageInfo)

                if (packageName != pkgName) appsList.add(App(appName, pkgName, appIcon, if (lockedAppsList.contains(pkgName)) "Locked" else "Unlocked"))
            }
        }

        appsList.sortBy { it.appName }
        id.appsList.adapter = AppAdaptor(this, appsList, type, lockedAppsList)
        id.appsList.layoutManager = LinearLayoutManager(this)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = getSystemService(VibratorManager::class.java)
            vibrator = vibratorManager.defaultVibrator
        }
        else vibrator = getSystemService(VIBRATOR_SERVICE) as Vibrator

        id.filterCard.setOnClickListener {
            if (type == "") {
                type = "filter"
                appsList.sortBy { it.status }
                id.filterCard.setCardBackgroundColor(getColor(R.color.grey_light))
            }
            else {
                type = ""
                appsList.sortBy { it.appName }
                id.filterCard.setCardBackgroundColor(getColor(R.color.transparent))
            }
            id.appsList.adapter = AppAdaptor(this, appsList, type, lockedAppsList)
        }

        id.locker.setOnClickListener { }
        id.changePass.setOnClickListener {
            id.locker.visibility = View.VISIBLE
            id.locker.startAnimation(AnimationUtils.loadAnimation(this, android.R.anim.fade_in))

            unlockWithPasscode("set")
        }

        id.materialSwitch.setOnCheckedChangeListener { _, b ->
            id.filterCard.setCardBackgroundColor(getColor(R.color.transparent))
            if (b) {
                if (!isUsagePermitted()) {
                    pausedFor = 1
                    type = "none"
                    ifLockedFile.writeText("false")
                    id.appsList.alpha = 0.5f

                    id.materialSwitch.isChecked = false
                    Toast.makeText(this, "Allow app to access usage stats.", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS, Uri.parse("package:$packageName")))
                }
                else if (!Settings.canDrawOverlays(this)) {
                    pausedFor = 2
                    type = "none"
                    ifLockedFile.writeText("false")
                    id.appsList.alpha = 0.5f

                    id.materialSwitch.isChecked = false
                    Toast.makeText(this, "Allow the app to overlay.", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
                }
                else {
                    type = ""
                    ifLockedFile.writeText("true")
                    id.appsList.alpha = 1f
                }
            }
            else {
                type = "none"
                ifLockedFile.writeText("false")
                id.appsList.alpha = 0.5f
            }
            id.appsList.adapter = AppAdaptor(this, appsList, type, lockedAppsList)
        }

        id.appsList.setOnScrollChangeListener { _, _, _, _, _ ->
            if (!id.appsList.canScrollVertically(1)) {
                id.viewBottom.visibility = View.GONE
                vibrate()
            }
            else if (!id.appsList.canScrollVertically(-1)) {
                id.viewTop.visibility = View.GONE
                vibrate()
            }
            else {
                id.viewTop.visibility = View.VISIBLE
                id.viewBottom.visibility = View.VISIBLE
            }
        }

        id.search.addTextChangedListener { query->
            if (query.toString().trim().isEmpty()) {
                type = ""
                id.filterCard.setCardBackgroundColor(getColor(R.color.transparent))
                id.appsList.adapter = AppAdaptor(this, appsList, type, lockedAppsList)
            }
            else {
                type = "filter"
                id.filterCard.setCardBackgroundColor(getColor(R.color.grey_light))

                val filteredList = ArrayList<App>()
                for (app in appsList) {
                    if (app.appName.lowercase().contains(query.toString().lowercase())) filteredList.add(app)
                    else if (app.packageName.lowercase().contains(query.toString().lowercase())) filteredList.add(app)
                }
                id.appsList.adapter = AppAdaptor(this, filteredList, type, lockedAppsList)
            }
        }
    }

    private fun vibrate() {
        if (vibrator.hasVibrator()) {
            vibrator.vibrate(VibrationEffect.createOneShot(25, VibrationEffect.DEFAULT_AMPLITUDE))
        }
    }

    data class App(
        val appName: String,
        val packageName: String,
        val icon: Drawable,
        var status: String
    )

    override fun onPause() {
        super.onPause()
        if (isServiceRunning()) stopService(Intent(this, ListenerService::class.java))
        if (lockedAppsList.isEmpty()) lockedFile.writeText("")
        else lockedFile.writeText(lockedAppsList.joinToString(","))
        startService(Intent(this, ListenerService::class.java))
    }

    private fun isServiceRunning(): Boolean {
        val activityManager = getSystemService(ACTIVITY_SERVICE) as ActivityManager
        val runningServices = activityManager.getRunningServices(Integer.MAX_VALUE)
        val serviceName = ListenerService::class.java.name

        for (serviceInfo in runningServices) {
            if (serviceInfo.service.className == serviceName) {
                return true
            }
        }
        return false
    }

    private fun isUsagePermitted(): Boolean {
        try {
            val appOps = getSystemService(APP_OPS_SERVICE) as AppOpsManager
            val mode = appOps.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), packageName)
            return mode == AppOpsManager.MODE_ALLOWED
        }
        catch (e: Exception) {
            e.printStackTrace()
        }
        return false
    }

    override fun onResume() {
        super.onResume()
        if (pausedFor == 1) {
            pausedFor = 0
            if (isUsagePermitted()) {
                if (!Settings.canDrawOverlays(this)) {
                    pausedFor = 2
                    Toast.makeText(this, "Allow the app to overlay.", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
                }
                else {
                    type = ""
                    ifLockedFile.writeText("true")

                    id.materialSwitch.isChecked = true
                    id.appsList.alpha = 1f
                    id.appsList.adapter = AppAdaptor(this, appsList, type, lockedAppsList)
                }
            }
            else Toast.makeText(this, "Permission denied.", Toast.LENGTH_SHORT).show()
        }
        else if (pausedFor == 2) {
            pausedFor = 0
            if (!Settings.canDrawOverlays(this)) Toast.makeText(this, "Permission denied.", Toast.LENGTH_SHORT).show()
            else {
                type = ""
                ifLockedFile.writeText("true")

                id.materialSwitch.isChecked = true
                id.appsList.alpha = 1f
                id.appsList.adapter = AppAdaptor(this, appsList, type, lockedAppsList)
            }
        }
    }

    @SuppressLint("SetTextI18n")
    @Deprecated("This method has been deprecated in favor of using the Activity Result API\n      which brings increased type safety via an {@link ActivityResultContract} and the prebuilt\n      contracts for common intents available in\n      {@link androidx.activity.result.contract.ActivityResultContracts}, provides hooks for\n      testing, and allow receiving results in separate, testable classes independent from your\n      activity. Use\n      {@link #registerForActivityResult(ActivityResultContract, ActivityResultCallback)}\n      with the appropriate {@link ActivityResultContract} and handling the result in the\n      {@link ActivityResultCallback#onActivityResult(Object) callback}.")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1) {
            if (resultCode != RESULT_OK) {
                Toast.makeText(this, "Authentication failed.", Toast.LENGTH_SHORT).show()
                finishAffinity()
            }
            else if (File(filesDir, "passcode.txt").exists()) {
                id.lockerLay.visibility = View.VISIBLE
                id.lockerLay.startAnimation(AnimationUtils.loadAnimation(this, android.R.anim.fade_in))
                unlockWithPasscode("")
            }
            else {
                id.textView4.text = "SET PASSCODE"
                id.locker.visibility = View.GONE
                id.lockerLay.visibility = View.VISIBLE
                id.locker.startAnimation(AnimationUtils.loadAnimation(this, android.R.anim.fade_out))
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun unlockWithPasscode(type: String, extra: String = "") {
        var passcode = ""
        var realPasscode = try { File(filesDir, "passcode.txt").readText() } catch (e: Exception) { "123456" }

        when (type) {
            "set" -> {
                id.textView23.text = "Set passcode"
                id.textView24.text = "Set your passcode to verify if it's really you who's trying to access locked app."
            }
            "confirm" -> {
                realPasscode = extra
                id.textView23.text = "Confirm passcode"
                id.textView24.text = "Confirm your passcode to verify if it's really you who's trying to access locked app."
            }
            else -> {
                id.textView23.text = "Enter passcode"
                id.textView24.text = "Enter your passcode to verify if it's really you who's trying to access app."
            }
        }

        id.done.setOnClickListener {
            vibrator.vibrate(25)
            if (passcode == realPasscode) {
                clearPasscode()

                if (type == "set") { unlockWithPasscode("confirm", passcode) }
                else {
                    if (type == "confirm") {
                        File(filesDir, "passcode.txt").writeText(passcode)
                        if (id.textView4.text.contains("SET")) {
                            id.textView4.text = "CHANGE PASSCODE"
                            Toast.makeText(applicationContext, "Passcode set successfully.", Toast.LENGTH_SHORT).show()
                        }
                        else Toast.makeText(applicationContext, "Passcode changed successfully.", Toast.LENGTH_SHORT).show()
                    }

                    id.locker.visibility = View.GONE
                    id.locker.startAnimation(AnimationUtils.loadAnimation(this, android.R.anim.fade_out))
                }
            }
            else {
                if (type == "confirm") {
                    clearPasscode()
                    unlockWithPasscode("set")
                    Toast.makeText(applicationContext, "Confirm passcode doesn't match.", Toast.LENGTH_SHORT).show()
                }
                else if (type == "set") {
                    if (passcode.length == 6) {
                        clearPasscode()
                        unlockWithPasscode("confirm", passcode)
                    }
                    else Toast.makeText(this, "Passcode must contain 6 digits.", Toast.LENGTH_SHORT).show()
                }
                else {
                    Toast.makeText(this, "Wrong passcode.", Toast.LENGTH_SHORT).show()
                    passcode = ""
                    clearPasscode()
                }
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

    private fun clearPasscode() {
        id.pass1.visibility = View.GONE
        id.pass2.visibility = View.GONE
        id.pass3.visibility = View.GONE
        id.pass4.visibility = View.GONE
        id.pass5.visibility = View.GONE
        id.pass6.visibility = View.GONE
    }

    @Deprecated("This method has been deprecated in favor of using the\n      {@link OnBackPressedDispatcher} via {@link #getOnBackPressedDispatcher()}.\n      The OnBackPressedDispatcher controls how back button events are dispatched\n      to one or more {@link OnBackPressedCallback} objects.")
    override fun onBackPressed() {
        if (id.locker.isVisible && !id.textView23.text.contains("Enter")) {
            id.locker.visibility = View.GONE
            id.locker.startAnimation(AnimationUtils.loadAnimation(this, android.R.anim.fade_out))
        }
        else super.onBackPressed()
    }
}