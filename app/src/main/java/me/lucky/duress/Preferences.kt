package me.lucky.duress

import android.content.Context
import androidx.core.content.edit
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys

class Preferences(ctx: Context) {
    companion object {
        private const val ENABLED = "enabled"
        private const val MODE = "mode"
        private const val ACTION = "action"
        private const val RECEIVER = "receiver"
        private const val AUTHENTICATION_CODE = "authentication_code"
        private const val PASSWORD_LEN = "password_len"
        private const val KEYGUARD_TYPE = "keyguard_type"
        private const val SHOW_PROMINENT_DISCLOSURE = "show_prominent_disclosure"

        private const val FILE_NAME = "sec_shared_prefs"
        // migration
        private const val SERVICE_ENABLED = "service_enabled"
    }

    private val mk = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
    private val prefs = EncryptedSharedPreferences.create(
        FILE_NAME,
        mk,
        ctx,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )

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

    var authenticationCode: String
        get() = prefs.getString(AUTHENTICATION_CODE, "") ?: ""
        set(value) = prefs.edit { putString(AUTHENTICATION_CODE, value) }

    var passwordLen: Int
        get() = prefs.getInt(PASSWORD_LEN, 0)
        set(value) = prefs.edit { putInt(PASSWORD_LEN, value) }

    var keyguardType: Int
        get() = prefs.getInt(KEYGUARD_TYPE, KeyguardType.A.value)
        set(value) = prefs.edit { putInt(KEYGUARD_TYPE, value) }

    var isShowProminentDisclosure: Boolean
        get() = prefs.getBoolean(SHOW_PROMINENT_DISCLOSURE, true)
        set(value) = prefs.edit { putBoolean(SHOW_PROMINENT_DISCLOSURE, value) }
}

enum class Mode(val value: Int) {
    BROADCAST(0),
    WIPE(1),
}

enum class KeyguardType(val value: Int) {
    A(0),
    B(1),
}
