package com.example.core.security

import android.content.Context
import android.content.SharedPreferences

object AppLockManager {
    private const val PREF_NAME = "aman_app_lock_prefs"
    private const val KEY_LOCK_ENABLED = "key_lock_enabled"
    private const val KEY_PIN_HASH = "key_pin_hash"
    private const val KEY_BIOMETRIC_ENABLED = "key_biometric_enabled"

    private var prefs: SharedPreferences? = null

    fun init(context: Context) {
        if (prefs == null) {
            prefs = context.applicationContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        }
    }

    fun isLockEnabled(): Boolean {
        return prefs?.getBoolean(KEY_LOCK_ENABLED, false) ?: false
    }

    fun hasPinSet(): Boolean {
        return !prefs?.getString(KEY_PIN_HASH, null).isNullOrBlank()
    }

    fun isBiometricEnabled(): Boolean = prefs?.getBoolean(KEY_BIOMETRIC_ENABLED, false) ?: false

    fun setBiometricEnabled(enabled: Boolean): Boolean {
        if (enabled && !hasPinSet()) return false
        prefs?.edit()?.putBoolean(KEY_BIOMETRIC_ENABLED, enabled)?.apply()
        return true
    }

    fun setPin(pin: String): Boolean {
        if (pin.length != 4 || !pin.all { it.isDigit() }) return false
        val hashed = hashPin(pin)
        prefs?.edit()?.putString(KEY_PIN_HASH, hashed)?.apply()
        return true
    }

    fun verifyPin(pin: String): Boolean {
        val storedHash = prefs?.getString(KEY_PIN_HASH, null) ?: return false
        return hashPin(pin) == storedHash
    }

    fun setLockEnabled(enabled: Boolean, pin: String? = null): Boolean {
        if (enabled) {
            if (pin != null) {
                if (!setPin(pin)) return false
            } else if (!hasPinSet()) {
                return false
            }
        }
        prefs?.edit()?.putBoolean(KEY_LOCK_ENABLED, enabled)?.apply()
        return true
    }

    private fun hashPin(pin: String): String {
        val bytes = java.security.MessageDigest.getInstance("SHA-256").digest(pin.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
