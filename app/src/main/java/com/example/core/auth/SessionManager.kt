package com.example.core.auth

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import com.example.data.model.AdminRole
import com.example.data.model.User
import com.example.data.model.UserType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

 data class AuthSession(val accessToken: String, val refreshToken: String?, val user: User)

object SessionManager {
    private const val PREF_NAME = "aman_auth_session"
    private const val KEY_ACCESS_TOKEN = "access_token"
    private const val KEY_REFRESH_TOKEN = "refresh_token"
    private const val KEY_USER_ID = "user_id"
    private const val KEY_USER_EMAIL = "user_email"
    private const val KEY_USER_NAME = "user_full_name"
    private const val KEY_USER_PHONE = "user_phone"
    private const val KEY_USER_TYPE = "user_type"
    private const val KEY_USER_ROLE = "user_role"
    private const val KEYSTORE = "AndroidKeyStore"
    private const val KEY_ALIAS = "aman_auth_session_key"
    private var prefs: SharedPreferences? = null
    private val _currentSession = MutableStateFlow<AuthSession?>(null)
    val currentSession: StateFlow<AuthSession?> = _currentSession.asStateFlow()

    fun init(context: Context) {
        if (prefs == null) {
            prefs = context.applicationContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            runCatching {
                ensureKey()
                restoreSession()
            }.onFailure {
                // A corrupt/unsupported keystore entry must not crash the launcher.
                _currentSession.value = null
                prefs?.edit()?.clear()?.apply()
            }
        }
    }

    private fun ensureKey() {
        val ks = KeyStore.getInstance(KEYSTORE).apply { load(null) }
        if (!ks.containsAlias(KEY_ALIAS)) {
            val generator = KeyGenerator.getInstance("AES", KEYSTORE)
            generator.init(
                KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setRandomizedEncryptionRequired(true)
                    .build()
            )
            generator.generateKey()
        }
    }

    private fun recreateKey() {
        val ks = KeyStore.getInstance(KEYSTORE).apply { load(null) }
        if (ks.containsAlias(KEY_ALIAS)) ks.deleteEntry(KEY_ALIAS)
        ensureKey()
    }

    private fun key(): SecretKey = (KeyStore.getInstance(KEYSTORE).apply { load(null) }.getKey(KEY_ALIAS, null) as SecretKey)

    private fun encrypt(value: String): String {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key())
        val combined = cipher.iv + cipher.doFinal(value.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(combined, Base64.NO_WRAP)
    }

    private fun decrypt(value: String): String? = try {
        val combined = Base64.decode(value, Base64.NO_WRAP)
        if (combined.size < 13) null else {
            val iv = combined.copyOfRange(0,12)
            val payload = combined.copyOfRange(12,combined.size)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE,key(),GCMParameterSpec(128,iv))
            String(cipher.doFinal(payload),Charsets.UTF_8)
        }
    } catch (_: Exception) { null }

    private fun restoreSession() {
        val p = prefs ?: return
        val token = p.getString(KEY_ACCESS_TOKEN, null)?.let(::decrypt)
        val refresh = p.getString(KEY_REFRESH_TOKEN, null)?.let(::decrypt)
        val id = p.getString(KEY_USER_ID, null)
        val email = p.getString(KEY_USER_EMAIL, null)
        if (token.isNullOrBlank() || id.isNullOrBlank() || email.isNullOrBlank()) { _currentSession.value=null; return }
        val type = if (p.getString(KEY_USER_TYPE, "CUSTOMER").equals("ADMIN", true)) UserType.ADMIN else UserType.CUSTOMER
        val role = if (type == UserType.ADMIN) AdminRole.fromCode(p.getString(KEY_USER_ROLE, null)) else null
        _currentSession.value = AuthSession(token,refresh,User(id,email,p.getString(KEY_USER_NAME,"مستخدم أمان") ?: "مستخدم أمان",p.getString(KEY_USER_PHONE,null),type,role,status="active"))
    }

    fun saveSession(accessToken: String, refreshToken: String?, user: User) {
        val editor = prefs?.edit()
        if (editor != null) {
            val encryptedAccess = runCatching { encrypt(accessToken) }.getOrElse {
                recreateKey()
                encrypt(accessToken)
            }
            val encryptedRefresh = refreshToken?.let { refresh ->
                runCatching { encrypt(refresh) }.getOrElse {
                    recreateKey()
                    encrypt(refresh)
                }
            }
            editor.putString(KEY_ACCESS_TOKEN, encryptedAccess)
            if (encryptedRefresh != null) editor.putString(KEY_REFRESH_TOKEN, encryptedRefresh) else editor.remove(KEY_REFRESH_TOKEN)
            editor.putString(KEY_USER_ID,user.id).putString(KEY_USER_EMAIL,user.email).putString(KEY_USER_NAME,user.fullName)
            editor.putString(KEY_USER_PHONE,user.phone).putString(KEY_USER_TYPE,user.userType.name).putString(KEY_USER_ROLE,user.role?.code).apply()
        }
        _currentSession.value=AuthSession(accessToken,refreshToken,user)
    }

    fun clearSession(){ prefs?.edit()?.clear()?.apply(); _currentSession.value=null }
    fun hasValidSession() = !_currentSession.value?.accessToken.isNullOrBlank()
    fun getAccessToken() = _currentSession.value?.accessToken
    fun getRefreshToken() = _currentSession.value?.refreshToken
    fun getCurrentUser() = _currentSession.value?.user
}
