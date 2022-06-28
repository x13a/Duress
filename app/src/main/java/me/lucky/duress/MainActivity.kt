package me.lucky.duress

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.view.accessibility.AccessibilityManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayout

import me.lucky.duress.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var prefs: Preferences
    private val admin by lazy { DeviceAdminManager(this) }
    private var accessibilityManager: AccessibilityManager? = null

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
            tabs.selectTab(tabs.getTabAt(prefs.mode))
            action.editText?.setText(prefs.action)
            receiver.editText?.setText(prefs.receiver)
            authenticationCode.editText?.setText(prefs.authenticationCode)
            passwordLen.editText?.setText(prefs.passwordLen.toString())
            keyguardType.check(when (prefs.keyguardType) {
                KeyguardType.A.value -> R.id.keyguardTypeA
                KeyguardType.B.value -> R.id.keyguardTypeB
                else -> R.id.keyguardTypeA
            })
            toggle.isChecked = prefs.isEnabled
        }
        selectInterface()
    }

    private fun setup() {
        binding.apply {
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
                prefs.action = it?.toString() ?: ""
            }
            receiver.editText?.doAfterTextChanged {
                prefs.receiver = it?.toString() ?: ""
            }
            authenticationCode.editText?.doAfterTextChanged {
                prefs.authenticationCode = it?.toString() ?: ""
            }
            passwordLen.editText?.doAfterTextChanged {
                try {
                    prefs.passwordLen = it?.toString()?.toInt() ?: return@doAfterTextChanged
                } catch (exc: NumberFormatException) {}
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
    }
    private fun selectInterface() {
        val v = when (prefs.mode) {
            Mode.BROADCAST.value -> View.VISIBLE
            Mode.WIPE.value -> View.GONE
            else -> View.GONE
        }
        binding.apply {
            action.visibility = v
            receiver.visibility = v
            authenticationCode.visibility = v
            space1.visibility = v
            space2.visibility = v
            space3.visibility = v
        }
    }

    private fun setOff() {
        prefs.isEnabled = false
        binding.toggle.isChecked = false
        try { admin.remove() } catch (exc: SecurityException) {}
    }

    private fun update() {
        if (prefs.isEnabled && !hasPermissions())
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
