package me.lucky.duress

import android.accessibilityservice.AccessibilityService
import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.view.accessibility.AccessibilityEvent
import java.lang.ref.WeakReference

import me.lucky.duress.admin.DeviceAdminManager

class AccessibilityService : AccessibilityService() {
    companion object {
        private const val MIN_PASSWORD_LEN = 6
        private const val KEY = "code"
        private const val BUTTON_DELETE_TEXT = "DELETE"
        private const val BUTTON_OK_TEXT = "OK"
    }

    private lateinit var prefs: Preferences
    private val admin by lazy { DeviceAdminManager(this) }
    private val lockReceiver = LockReceiver(WeakReference(this))
    private var keyguardManager: KeyguardManager? = null
    private var enteredPwLen = 0

    override fun onCreate() {
        super.onCreate()
        init()
    }

    override fun onDestroy() {
        super.onDestroy()
        deinit()
    }

    private fun init() {
        prefs = Preferences.new(this)
        keyguardManager = getSystemService(KeyguardManager::class.java)
        registerReceiver(lockReceiver, IntentFilter().apply {
            addAction(Intent.ACTION_USER_PRESENT)
            addAction(Intent.ACTION_SCREEN_OFF)
        })
    }

    private fun deinit() {
        unregisterReceiver(lockReceiver)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (keyguardManager?.isDeviceLocked != true ||
            event?.isEnabled != true ||
            !prefs.isEnabled) return
        if (!when (prefs.keyguardType) {
            KeyguardType.A.value -> checkKeyguardTypeA(event)
            KeyguardType.B.value -> checkKeyguardTypeB(event)
            else -> return
        }) return
        when (prefs.mode) {
            Mode.TEST.value -> sendNotification()
            Mode.WIPE.value -> wipeData()
            Mode.BROADCAST.value -> sendBroadcast()
        }
    }

    override fun onInterrupt() {}

    override fun onServiceConnected() {
        super.onServiceConnected()
        if (prefs.keyguardType != KeyguardType.B.value) return
        serviceInfo = serviceInfo.apply {
            eventTypes = AccessibilityEvent.TYPE_VIEW_CLICKED or
                AccessibilityEvent.TYPE_VIEW_LONG_CLICKED
        }
    }

    private fun checkKeyguardTypeA(event: AccessibilityEvent): Boolean {
        if (event.eventType != AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED ||
            !event.isPassword) return false
        val passwordLen = prefs.passwordLen
        if (passwordLen < MIN_PASSWORD_LEN ||
            event.text.size != 1 ||
            event.text[0].length < passwordLen) return false
        return true
    }

    private fun checkKeyguardTypeB(event: AccessibilityEvent): Boolean {
        if (event.eventType != AccessibilityEvent.TYPE_VIEW_CLICKED &&
            event.eventType != AccessibilityEvent.TYPE_VIEW_LONG_CLICKED) return false
        val passwordLen = prefs.passwordLen
        if (passwordLen < MIN_PASSWORD_LEN || event.text.size != 1) return false
        when (event.contentDescription.toString()) {
            BUTTON_DELETE_TEXT -> {
                if (event.eventType == AccessibilityEvent.TYPE_VIEW_LONG_CLICKED) enteredPwLen = 0
                else if (enteredPwLen > 0) enteredPwLen -= 1
            }
            BUTTON_OK_TEXT -> enteredPwLen = 0
            else -> enteredPwLen += 1
        }
        if (enteredPwLen < passwordLen) return false
        return true
    }

    private fun sendNotification() = NotificationManager(this).send()

    private fun sendBroadcast() {
        val action = prefs.action
        if (action.isEmpty()) return
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
            val secret = prefs.secret
            if (secret.isNotEmpty()) putExtra(KEY, secret)
        })
    }

    private fun wipeData() {
        try { admin.wipeData() } catch (exc: SecurityException) {}
    }

    private class LockReceiver(
        private val service: WeakReference<me.lucky.duress.AccessibilityService>,
    ) : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action != Intent.ACTION_USER_PRESENT &&
                intent?.action != Intent.ACTION_SCREEN_OFF) return
            service.get()?.enteredPwLen = 0
        }
    }
}