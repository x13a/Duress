package me.lucky.duress

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.os.UserManager
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys

class Preferences(ctx: Context, encrypted: Boolean = true) {
    companion object {
        private const val ENABLED = "enabled"
        private const val MODE = "mode"
        private const val ACTION = "action"
        private const val RECEIVER = "receiver"
        private const val SECRET = "secret"
        private const val PASSWORD_OR_LEN = "password_or_len"
        private const val KEYGUARD_TYPE = "keyguard_type"
        private const val SHOW_PROMINENT_DISCLOSURE = "show_prominent_disclosure"

        private const val FILE_NAME = "sec_shared_prefs"
        // migration
        private const val SERVICE_ENABLED = "service_enabled"
        private const val AUTHENTICATION_CODE = "authentication_code"
        private const val PASSWORD_LEN = "password_len"

        fun new(ctx: Context) = Preferences(
            ctx,
            encrypted = Build.VERSION.SDK_INT < Build.VERSION_CODES.N ||
                ctx.getSystemService(UserManager::class.java).isUserUnlocked,
        )
    }

    private val prefs: SharedPreferences = if (encrypted) {
        val mk = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
        EncryptedSharedPreferences.create(
            FILE_NAME,
            mk,
            ctx,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    } else {
        val context = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
            ctx.createDeviceProtectedStorageContext() else ctx
        PreferenceManager.getDefaultSharedPreferences(context)
    }

    var isEnabled: Boolean
        get() = prefs.getBoolean(ENABLED, prefs.getBoolean(SERVICE_ENABLED, false))
        set(value) = prefs.edit { putBoolean(ENABLED, value) }

    var mode: Int
        get() = prefs.getInt(MODE, Mode.BROADCAST.value)
        set(value) = prefs.edit { putInt(MODE, value) }

    var action: String
        get() = prefs.getString(ACTION, "") ?: ""
        set(value) = prefs.edit { putString(ACTION, value) }

    var receiver: String
        get() = prefs.getString(RECEIVER, "") ?: ""
        set(value) = prefs.edit { putString(RECEIVER, value) }

    var secret: String
        get() = prefs.getString(SECRET, prefs.getString(AUTHENTICATION_CODE, "")) ?: ""
        set(value) = prefs.edit { putString(SECRET, value) }

    var passwordOrLen: String
        get() = prefs.getString(PASSWORD_OR_LEN, prefs.getString(PASSWORD_LEN, "")) ?: ""
        set(value) = prefs.edit { putString(PASSWORD_OR_LEN, value) }

    var keyguardType: Int
        get() = prefs.getInt(KEYGUARD_TYPE, KeyguardType.A.value)
        set(value) = prefs.edit { putInt(KEYGUARD_TYPE, value) }

    var isShowProminentDisclosure: Boolean
        get() = prefs.getBoolean(SHOW_PROMINENT_DISCLOSURE, true)
        set(value) = prefs.edit { putBoolean(SHOW_PROMINENT_DISCLOSURE, value) }

    fun registerListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) =
        prefs.registerOnSharedPreferenceChangeListener(listener)

    fun unregisterListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) =
        prefs.unregisterOnSharedPreferenceChangeListener(listener)

    fun copyTo(dst: Preferences, key: String? = null) = dst.prefs.edit {
        for (entry in prefs.all.entries) {
            val k = entry.key
            if (key != null && k != key) continue
            val v = entry.value ?: continue
            when (v) {
                is Boolean -> putBoolean(k, v)
                is Int -> putInt(k, v)
                is String -> putString(k, v)
            }
        }
    }
}

enum class Mode(val value: Int) {
    BROADCAST(0),
    WIPE(1),
    TEST(2),
}

enum class KeyguardType(val value: Int) {
    A(0),
    B(1),
}