package me.lucky.duress

import android.content.Context
import androidx.core.content.edit
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys

class Preferences(ctx: Context) {
    companion object {
        private const val SERVICE_ENABLED = "service_enabled"
        private const val ACTION = "action"
        private const val RECEIVER = "receiver"
        private const val AUTHENTICATION_CODE = "authentication_code"
        private const val PASSWORD_LEN = "password_len"
        private const val SHOW_PROMINENT_DISCLOSURE = "show_prominent_disclosure"

        private const val FILE_NAME = "sec_shared_prefs"
    }

    private val mk = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
    private val prefs = EncryptedSharedPreferences.create(
        FILE_NAME,
        mk,
        ctx,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )

    var isServiceEnabled: Boolean
        get() = prefs.getBoolean(SERVICE_ENABLED, false)
        set(value) = prefs.edit { putBoolean(SERVICE_ENABLED, value) }

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

    var isShowProminentDisclosure: Boolean
        get() = prefs.getBoolean(SHOW_PROMINENT_DISCLOSURE, true)
        set(value) = prefs.edit { putBoolean(SHOW_PROMINENT_DISCLOSURE, value) }
}
