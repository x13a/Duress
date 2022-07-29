package me.lucky.duress

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.view.accessibility.AccessibilityManager
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.core.widget.doAfterTextChanged
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayout

import me.lucky.duress.admin.DeviceAdminManager
import me.lucky.duress.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var prefs: Preferences
    private lateinit var prefsdb: Preferences
    private val admin by lazy { DeviceAdminManager(this) }
    private var accessibilityManager: AccessibilityManager? = null

    private val prefsListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        prefs.copyTo(prefsdb, key)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        init1()
        if (initBiometric()) return
        init2()
        setup()
        if (prefs.isShowProminentDisclosure) showProminentDisclosure()
    }

    override fun onStart() {
        super.onStart()
        prefs.registerListener(prefsListener)
        update()
    }

    override fun onStop() {
        super.onStop()
        prefs.unregisterListener(prefsListener)
    }

    private fun init1() {
        prefs = Preferences(this)
        prefsdb = Preferences(this, encrypted = false)
        prefs.copyTo(prefsdb)
        accessibilityManager = getSystemService(AccessibilityManager::class.java)
    }

    private fun init2() {
        selectInterface()
        binding.apply {
            tabs.selectTab(tabs.getTabAt(prefs.mode))
            action.editText?.setText(prefs.action)
            receiver.editText?.setText(prefs.receiver)
            extraKey.editText?.setText(prefs.extraKey)
            extraValue.editText?.setText(prefs.extraValue)
            passwordOrLen.editText?.setText(prefs.passwordOrLen)
            keyguardType.check(when (prefs.keyguardType) {
                KeyguardType.A.value -> R.id.keyguardTypeA
                KeyguardType.B.value -> R.id.keyguardTypeB
                else -> R.id.keyguardTypeA
            })
            toggle.isChecked = prefs.isEnabled
        }
    }

    private fun initBiometric(): Boolean {
        val authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG or
                BiometricManager.Authenticators.DEVICE_CREDENTIAL
        when (BiometricManager
            .from(this)
            .canAuthenticate(authenticators))
        {
            BiometricManager.BIOMETRIC_SUCCESS -> {}
            else -> return false
        }
        val executor = ContextCompat.getMainExecutor(this)
        val prompt = BiometricPrompt(
            this,
            executor,
            object : BiometricPrompt.AuthenticationCallback()
        {
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                finishAndRemoveTask()
            }

            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                init2()
                setup()
                if (prefs.isShowProminentDisclosure) showProminentDisclosure()
            }
        })
        try {
            prompt.authenticate(BiometricPrompt.PromptInfo.Builder()
                .setTitle(getString(R.string.authentication))
                .setConfirmationRequired(false)
                .setAllowedAuthenticators(authenticators)
                .build())
        } catch (exc: Exception) { return false }
        return true
    }

    private fun setup() = binding.apply {
        tabs.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                if (tab == null) return
                setOff()
                for (m in Mode.values()) {
                    if (m.value == tab.position) {
                        prefs.mode = m.value
                        break
                    }
                }
                selectInterface()
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}

        })
        action.editText?.doAfterTextChanged {
            prefs.action = it?.toString()?.trim() ?: ""
        }
        receiver.editText?.doAfterTextChanged {
            prefs.receiver = it?.toString()?.trim() ?: ""
        }
        extraKey.editText?.doAfterTextChanged {
            prefs.extraKey = it?.toString()?.trim() ?: ""
        }
        extraValue.editText?.doAfterTextChanged {
            prefs.extraValue = it?.toString()?.trim() ?: ""
        }
        passwordOrLen.editText?.doAfterTextChanged {
            prefs.passwordOrLen = it?.toString()?.trim() ?: ""
        }
        keyguardType.setOnCheckedChangeListener { _, checkedId ->
            prefs.keyguardType = when (checkedId) {
                R.id.keyguardTypeA -> KeyguardType.A.value
                R.id.keyguardTypeB -> KeyguardType.B.value
                else -> return@setOnCheckedChangeListener
            }
        }
        toggle.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked && !hasPermissions()) {
                toggle.isChecked = false
                requestPermissions()
                return@setOnCheckedChangeListener
            }
            prefs.isEnabled = isChecked
        }
    }

    private fun selectInterface() {
        val v = when (prefs.mode) {
            Mode.BROADCAST.value -> View.VISIBLE
            Mode.WIPE.value -> View.GONE
            Mode.TEST.value -> {
                NotificationManager(this).createNotificationChannels()
                View.GONE
            }
            else -> View.GONE
        }
        binding.apply {
            action.visibility = v
            receiver.visibility = v
            extraKey.visibility = v
            extraValue.visibility = v
            space1.visibility = v
            space2.visibility = v
            space3.visibility = v
            space4.visibility = v
        }
    }

    private fun setOff() {
        prefs.isEnabled = false
        try { admin.remove() } catch (exc: SecurityException) {}
        binding.toggle.isChecked = false
    }

    private fun update() {
        if (prefs.isEnabled && !hasPermissions())
            Snackbar.make(
                binding.toggle,
                R.string.service_unavailable_popup,
                Snackbar.LENGTH_SHORT,
            ).show()
    }

    private fun showProminentDisclosure() =
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.prominent_disclosure_title)
            .setMessage(R.string.prominent_disclosure_message)
            .setPositiveButton(R.string.accept) { _, _ ->
                prefs.isShowProminentDisclosure = false
            }
            .setNegativeButton(R.string.exit) { _, _ ->
                finishAndRemoveTask()
            }
            .show()

    private fun requestPermissions() {
        if (!hasAccessibilityPermission()) {
            requestAccessibilityPermission()
            return
        }
        if (prefs.mode == Mode.WIPE.value && !hasAdminPermission()) requestAdminPermission()
    }

    private fun requestAccessibilityPermission() =
        startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))

    private fun requestAdminPermission() = startActivity(admin.makeRequestIntent())

    private fun hasPermissions(): Boolean {
        var ok = hasAccessibilityPermission()
        if (prefs.mode == Mode.WIPE.value)
            ok = ok && hasAdminPermission()
        return ok
    }

    private fun hasAccessibilityPermission(): Boolean {
        for (info in accessibilityManager?.getEnabledAccessibilityServiceList(
            AccessibilityServiceInfo.FEEDBACK_GENERIC,
        ) ?: return true) {
            if (info.resolveInfo.serviceInfo.packageName == packageName) return true
        }
        return false
    }

    private fun hasAdminPermission() = admin.isActive()
}