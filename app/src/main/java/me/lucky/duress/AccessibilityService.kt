package me.lucky.duress

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
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
        private const val MIN_KEYGUARD_LEN = 4
        private const val MIN_LEN = MIN_KEYGUARD_LEN + 2
        private const val KEY = "code"
        private const val BUTTON_DELETE_DESC = "delete"
        private const val BUTTON_OK_DESC = "ok"
        private const val BUTTON_ENTER_DESC = "enter"
        private const val WRONG_TEXT = "wrong"
        private const val INCORRECT_TEXT = "incorrect"
        private const val IGNORE_CHAR = '•'
    }

    private lateinit var prefs: Preferences
    private val admin by lazy { DeviceAdminManager(this) }
    private val lockReceiver = LockReceiver(WeakReference(this))
    private var keyguardManager: KeyguardManager? = null
    private var pos = 0
    private var counter = mutableListOf<Boolean>()

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
        val kg = prefs.keyguardType
        if (kg == KeyguardType.A.value)
            serviceInfo = serviceInfo.apply {
                eventTypes = AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED or
                    AccessibilityEvent.TYPE_VIEW_TEXT_SELECTION_CHANGED
                flags = AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
            }
        else if (kg == KeyguardType.B.value)
            serviceInfo = serviceInfo.apply {
                eventTypes = AccessibilityEvent.TYPE_VIEW_CLICKED or
                    AccessibilityEvent.TYPE_VIEW_LONG_CLICKED or
                    AccessibilityEvent.TYPE_ANNOUNCEMENT
            }
    }

    private fun checkKeyguardTypeA(event: AccessibilityEvent): Boolean {
        if (event.eventType != AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED &&
            event.eventType != AccessibilityEvent.TYPE_VIEW_TEXT_SELECTION_CHANGED) return false
        val passwordOrLen = prefs.passwordOrLen
        return when (passwordOrLen.length < MIN_KEYGUARD_LEN) {
            true -> checkKeyguardTypeAbyLen(event, passwordOrLen.toIntOrNull() ?: return false)
            false -> checkKeyguardTypeAbyPassword(event, passwordOrLen)
        }
    }

    private fun checkKeyguardTypeAbyLen(event: AccessibilityEvent, len: Int) =
        len >= MIN_LEN && event.text.size == 1 && event.text[0].length >= len

    private fun checkKeyguardTypeAbyPassword(event: AccessibilityEvent, pw: String): Boolean {
        if (event.text.isEmpty()) {
            reset()
            return false
        }
        val text = event.text[0]
        if (pos > text.length) {
            if (pos > 0) {
                pos--
                counter.removeAt(pos)
            }
            return false
        }
        val c = text.elementAtOrNull(pos) ?: return false
        if (c == IGNORE_CHAR) return false
        var ok = false
        counter.add(pos < pw.length && pw[pos] == c)
        pos++
        if (pos == pw.length) ok = counter.all { it }
        return ok
    }

    private fun checkKeyguardTypeB(event: AccessibilityEvent): Boolean {
        if (event.eventType != AccessibilityEvent.TYPE_VIEW_CLICKED &&
            event.eventType != AccessibilityEvent.TYPE_VIEW_LONG_CLICKED &&
            event.eventType != AccessibilityEvent.TYPE_ANNOUNCEMENT) return false
        val passwordOrLen = prefs.passwordOrLen
        return when (passwordOrLen.length < MIN_KEYGUARD_LEN) {
            true -> checkKeyguardTypeBbyLen(event, passwordOrLen.toIntOrNull() ?: return false)
            false -> checkKeyguardTypeBbyPassword(event, passwordOrLen)
        }
    }

    private fun checkKeyguardTypeBbyLen(event: AccessibilityEvent, len: Int): Boolean {
        if (len < MIN_LEN) return false
        var ok = false
        if (event.eventType == AccessibilityEvent.TYPE_ANNOUNCEMENT) {
            if (event.text.size != 1) return false
            val text = event.text[0]
            if (text.startsWith(WRONG_TEXT, true) ||
                text.startsWith(INCORRECT_TEXT, true))
            {
                ok = pos >= len
                pos = 0
            }
            return ok
        }
        when (event.contentDescription?.toString()?.lowercase()) {
            BUTTON_DELETE_DESC -> {
                if (event.eventType == AccessibilityEvent.TYPE_VIEW_LONG_CLICKED) pos = 0
                else if (pos > 0) pos--
            }
            BUTTON_OK_DESC, BUTTON_ENTER_DESC -> {
                ok = pos >= len
                pos = 0
            }
            null -> pos = 0
            else -> {
                pos++
                ok = pos >= len
            }
        }
        return ok
    }

    private fun checkKeyguardTypeBbyPassword(event: AccessibilityEvent, pw: String): Boolean {
        var ok = false
        if (event.eventType == AccessibilityEvent.TYPE_ANNOUNCEMENT) {
            if (event.text.size != 1) return false
            val text = event.text[0]
            if (text.startsWith(WRONG_TEXT, true) ||
                text.startsWith(INCORRECT_TEXT, true))
            {
                if (pos == pw.length) ok = counter.all { it }
                reset()
            }
            return ok
        }
        when (event.contentDescription?.toString()?.lowercase()) {
            BUTTON_DELETE_DESC -> {
                if (event.eventType == AccessibilityEvent.TYPE_VIEW_LONG_CLICKED) {
                    reset()
                } else if (pos > 0) {
                    pos--
                    counter.removeAt(pos)
                }
            }
            BUTTON_OK_DESC, BUTTON_ENTER_DESC -> {
                if (pos == pw.length) ok = counter.all { it }
                reset()
            }
            null -> reset()
            else -> {
                counter.add(pos < pw.length && pw[pos] == event.contentDescription.firstOrNull())
                pos++
                if (pos == pw.length) ok = counter.all { it }
            }
        }
        return ok
    }

    private fun reset() {
        pos = 0
        counter.clear()
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

    private fun wipeData() = try { admin.wipeData() } catch (exc: SecurityException) {}

    private class LockReceiver(
        private val service: WeakReference<me.lucky.duress.AccessibilityService>,
    ) : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action != Intent.ACTION_USER_PRESENT &&
                intent?.action != Intent.ACTION_SCREEN_OFF) return
            service.get()?.reset()
        }
    }
}