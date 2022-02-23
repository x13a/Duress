package me.lucky.duress

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar

import me.lucky.duress.databinding.ActivityMainBinding
import java.util.regex.Pattern

class MainActivity : AppCompatActivity() {
    companion object {
        private const val ACTION_REGEX = "\\w+(\\.\\w+)+"
    }

    private lateinit var binding: ActivityMainBinding
    private lateinit var prefs: Preferences
    private var accessibilityManager: AccessibilityManager? = null
    private val actionPatternRegex by lazy { Pattern.compile("^$ACTION_REGEX\$") }
    private val receiverPatternRegex by lazy { Pattern.compile("^$ACTION_REGEX/\\w+$") }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        init()
        setup()
        if (prefs.isShowProminentDisclosure) showProminentDisclosure()
    }

    override fun onStart() {
        super.onStart()
        update()
    }

    private fun init() {
        prefs = Preferences(this)
        accessibilityManager = getSystemService(AccessibilityManager::class.java)
        binding.apply {
            action.editText?.setText(prefs.action)
            receiver.editText?.setText(prefs.receiver)
            authenticationCode.editText?.setText(prefs.authenticationCode)
            passwordLen.editText?.setText(prefs.passwordLen.toString())
            toggle.isChecked = prefs.isServiceEnabled
        }
    }

    private fun setup() {
        binding.apply {
            action.editText?.doAfterTextChanged {
                val str = it?.toString() ?: ""
                if (actionPatternRegex.matcher(str).matches()) {
                    prefs.action = str
                    action.error = null
                } else {
                    action.error = getString(R.string.action_error)
                }
            }
            receiver.editText?.doAfterTextChanged {
                val str = it?.toString() ?: ""
                if (receiverPatternRegex.matcher(str).matches()) {
                    prefs.receiver = str
                    receiver.error = null
                } else {
                    receiver.error = getString(R.string.receiver_error)
                }
            }
            authenticationCode.editText?.doAfterTextChanged {
                val str = it?.toString() ?: ""
                if (str.isNotBlank()) {
                    prefs.authenticationCode = str
                    authenticationCode.error = null
                } else {
                    authenticationCode.error = getString(R.string.authentication_code_error)
                }
            }
            passwordLen.editText?.doAfterTextChanged {
                try {
                    prefs.passwordLen = it?.toString()?.toInt() ?: return@doAfterTextChanged
                } catch (exc: NumberFormatException) {}
            }
            toggle.setOnCheckedChangeListener { _, isChecked ->
                prefs.isServiceEnabled = isChecked
                if (isChecked && !hasPermissions())
                    startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            }
        }
    }

    private fun update() {
        if (prefs.isServiceEnabled && !hasPermissions())
            Snackbar.make(
                binding.toggle,
                R.string.service_unavailable_popup,
                Snackbar.LENGTH_SHORT,
            ).show()
    }

    private fun showProminentDisclosure() {
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
    }

    private fun hasPermissions(): Boolean {
        for (info in accessibilityManager?.getEnabledAccessibilityServiceList(
            AccessibilityServiceInfo.FEEDBACK_GENERIC,
        ) ?: return true) {
            if (info.resolveInfo.serviceInfo.packageName == packageName) return true
        }
        return false
    }
}
