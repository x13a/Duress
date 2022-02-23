package me.lucky.duress

import android.accessibilityservice.AccessibilityService
import android.app.KeyguardManager
import android.content.Intent
import android.view.accessibility.AccessibilityEvent

class AccessibilityService : AccessibilityService() {
    companion object {
        private const val MIN_PASSWORD_LEN = 6
        private const val KEY = "code"
    }

    private lateinit var prefs: Preferences
    private val admin by lazy { DeviceAdminManager(this) }
    private var keyguardManager: KeyguardManager? = null

    override fun onCreate() {
        super.onCreate()
        init()
    }

    private fun init() {
        prefs = Preferences(this)
        keyguardManager = getSystemService(KeyguardManager::class.java)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.isEnabled != true ||
            !event.isPassword ||
            keyguardManager?.isDeviceLocked != true ||
            !prefs.isEnabled) return
        val passwordLen = prefs.passwordLen
        if (passwordLen < MIN_PASSWORD_LEN ||
            event.text.size != 1 ||
            event.text[0].length < passwordLen) return
        if (prefs.mode == Mode.WIPE.value) {
            wipeData()
            return
        }
        val action = prefs.action
        if (action.isEmpty()) return
        sendBroadcast(action)
    }

    override fun onInterrupt() {}

    private fun sendBroadcast(action: String) {
        sendBroadcast(Intent(action).apply {
            val cls = prefs.receiver.split('/')
            val packageName = cls.firstOrNull() ?: ""
            if (packageName.isNotEmpty()) {
                setPackage(packageName)
                if (cls.size == 2)
                    setClassName(
                        packageName,
                        "$packageName.${cls[1].trimStart('.')}",
                    )
            }
            addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES)
            val code = prefs.authenticationCode
            if (code.isNotEmpty()) putExtra(KEY, code)
        })
    }

    private fun wipeData() {
        try { admin.wipeData() } catch (exc: SecurityException) {}
    }
}
