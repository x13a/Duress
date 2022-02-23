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
        if (event?.isPassword != true ||
            keyguardManager?.isDeviceLocked != true ||
            !prefs.isServiceEnabled) return
        val passwordLen = prefs.passwordLen
        if (passwordLen < MIN_PASSWORD_LEN ||
            event.text.size != 1 ||
            event.text[0].length < passwordLen) return
        val action = prefs.action
        if (action.isBlank()) return
        sendBroadcast(Intent(action).apply {
            val cls = prefs.receiver.split('/')
            val packageName = cls.firstOrNull() ?: ""
            if (packageName.isNotBlank()) {
                setPackage(packageName)
                if (cls.size == 2) setClassName(packageName, "$packageName.${cls[1]}")
            }
            addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES)
            val code = prefs.authenticationCode
            if (code.isNotBlank()) putExtra(KEY, code)
        })
    }

    override fun onInterrupt() {}
}
