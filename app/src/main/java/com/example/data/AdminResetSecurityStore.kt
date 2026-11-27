package com.example.data

import android.content.Context
import android.util.Base64
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

private val Context.adminSecurityDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "admin_reset_security_prefs"
)

enum class AdminVerificationMethod(val label: String, val description: String) {
    ADMIN_PIN(
        label = "Admin PIN",
        description = "Verify with a secret Admin PIN code configured by admin"
    ),
    GOOGLE_ADMIN(
        label = "Google Admin Re-Auth",
        description = "Re-verify the signed-in Google Admin account"
    ),
    PIN_AND_GOOGLE(
        label = "PIN + Google Admin",
        description = "Require both Admin PIN and Google Admin re-verification"
    );

    companion object {
        fun fromRaw(raw: String?): AdminVerificationMethod =
            entries.firstOrNull { it.name == raw } ?: ADMIN_PIN
    }
}

data class AdminSecurityConfig(
    val adminEmail: String = "",
    val method: AdminVerificationMethod = AdminVerificationMethod.ADMIN_PIN,
    val isConfigured: Boolean = false,
    val failedAttempts: Int = 0,
    val lockoutUntilEpochMs: Long = 0L
) {
    val requiresPin: Boolean
        get() = method == AdminVerificationMethod.ADMIN_PIN || method == AdminVerificationMethod.PIN_AND_GOOGLE

    val requiresGoogleReauth: Boolean
        get() = method == AdminVerificationMethod.GOOGLE_ADMIN || method == AdminVerificationMethod.PIN_AND_GOOGLE
}

sealed interface PinVerifyResult {
    data object Verified : PinVerifyResult
    data object NotConfigured : PinVerifyResult
    data class Invalid(val attemptsRemaining: Int) : PinVerifyResult
    data class LockedOut(val secondsRemaining: Long) : PinVerifyResult
}

object AdminResetSecurityStore {
    private val KEY_ADMIN_EMAIL = stringPreferencesKey("admin_email")
    private val KEY_METHOD = stringPreferencesKey("verification_method")
    private val KEY_CONFIGURED = booleanPreferencesKey("security_configured")
    private val KEY_PIN_HASH = stringPreferencesKey("admin_pin_hash")
    private val KEY_PIN_SALT = stringPreferencesKey("admin_pin_salt")
    private val KEY_FAILED_ATTEMPTS = intPreferencesKey("failed_attempts")
    private val KEY_LOCKOUT_UNTIL_MS = longPreferencesKey("lockout_until_ms")

    private const val PBKDF2_ITERATIONS = 120_000
    private const val KEY_LENGTH_BITS = 256
    private const val MAX_FAILED_ATTEMPTS = 5
    private const val LOCKOUT_DURATION_MS = 30_000L

    fun observeConfig(context: Context, defaultAdminEmail: String): Flow<AdminSecurityConfig> =
        context.applicationContext.adminSecurityDataStore.data.map { prefs ->
            val method = AdminVerificationMethod.fromRaw(prefs[KEY_METHOD])
            val hasPin = !prefs[KEY_PIN_HASH].isNullOrBlank() && !prefs[KEY_PIN_SALT].isNullOrBlank()
            val savedConfigured = prefs[KEY_CONFIGURED] ?: false
            val needsPin = method != AdminVerificationMethod.GOOGLE_ADMIN
            AdminSecurityConfig(
                adminEmail = prefs[KEY_ADMIN_EMAIL]?.takeIf { it.isNotBlank() } ?: defaultAdminEmail,
                method = method,
                isConfigured = savedConfigured && (!needsPin || hasPin),
                failedAttempts = prefs[KEY_FAILED_ATTEMPTS] ?: 0,
                lockoutUntilEpochMs = prefs[KEY_LOCKOUT_UNTIL_MS] ?: 0L
            )
        }

    suspend fun configureAdminSecurity(
        context: Context,
        adminEmail: String,
        method: AdminVerificationMethod,
        newPinOrNull: String?
    ): Result<Unit> {
        val cleanEmail = adminEmail.trim()
        if (cleanEmail.isBlank() || !cleanEmail.contains("@")) {
            return Result.failure(IllegalArgumentException("Enter a valid admin email address."))
        }

        val appCtx = context.applicationContext
        val existingPrefs = appCtx.adminSecurityDataStore.data.first()
        val hasExistingPin = !existingPrefs[KEY_PIN_HASH].isNullOrBlank() && !existingPrefs[KEY_PIN_SALT].isNullOrBlank()
        val needsPin = method == AdminVerificationMethod.ADMIN_PIN || method == AdminVerificationMethod.PIN_AND_GOOGLE

        val newPin = newPinOrNull?.trim().orEmpty()
        if (needsPin && newPin.isEmpty() && !hasExistingPin) {
            return Result.failure(IllegalArgumentException("Enter a 4–12 character Admin PIN."))
        }
        if (newPin.isNotEmpty() && newPin.length !in 4..12) {
            return Result.failure(IllegalArgumentException("Admin PIN must be 4–12 characters."))
        }

        appCtx.adminSecurityDataStore.edit { prefs ->
            prefs[KEY_ADMIN_EMAIL] = cleanEmail
            prefs[KEY_METHOD] = method.name
            prefs[KEY_CONFIGURED] = true
            prefs[KEY_FAILED_ATTEMPTS] = 0
            prefs[KEY_LOCKOUT_UNTIL_MS] = 0L
            if (newPin.isNotEmpty()) {
                val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
                val hash = derivePinHash(newPin, salt)
                prefs[KEY_PIN_SALT] = Base64.encodeToString(salt, Base64.NO_WRAP)
                prefs[KEY_PIN_HASH] = Base64.encodeToString(hash, Base64.NO_WRAP)
            }
        }
        return Result.success(Unit)
    }

    suspend fun verifyPin(context: Context, candidatePin: String): PinVerifyResult {
        val appCtx = context.applicationContext
        val prefs = appCtx.adminSecurityDataStore.data.first()
        val saltB64 = prefs[KEY_PIN_SALT]
        val hashB64 = prefs[KEY_PIN_HASH]
        if (saltB64.isNullOrBlank() || hashB64.isNullOrBlank()) {
            return PinVerifyResult.NotConfigured
        }

        val now = System.currentTimeMillis()
        val lockoutUntil = prefs[KEY_LOCKOUT_UNTIL_MS] ?: 0L
        if (lockoutUntil > now) {
            val secs = ((lockoutUntil - now + 999L) / 1000L).coerceAtLeast(1L)
            return PinVerifyResult.LockedOut(secs)
        }

        val salt = Base64.decode(saltB64, Base64.NO_WRAP)
        val expectedHash = Base64.decode(hashB64, Base64.NO_WRAP)
        val candidateHash = derivePinHash(candidatePin.trim(), salt)
        val matched = MessageDigest.isEqual(expectedHash, candidateHash)

        if (matched) {
            appCtx.adminSecurityDataStore.edit { editPrefs ->
                editPrefs[KEY_FAILED_ATTEMPTS] = 0
                editPrefs[KEY_LOCKOUT_UNTIL_MS] = 0L
            }
            return PinVerifyResult.Verified
        }

        val previousAttempts = if (lockoutUntil in 1L..now) 0 else (prefs[KEY_FAILED_ATTEMPTS] ?: 0)
        val nextAttempts = previousAttempts + 1
        return if (nextAttempts >= MAX_FAILED_ATTEMPTS) {
            appCtx.adminSecurityDataStore.edit { editPrefs ->
                editPrefs[KEY_FAILED_ATTEMPTS] = 0
                editPrefs[KEY_LOCKOUT_UNTIL_MS] = now + LOCKOUT_DURATION_MS
            }
            PinVerifyResult.LockedOut(LOCKOUT_DURATION_MS / 1000L)
        } else {
            appCtx.adminSecurityDataStore.edit { editPrefs ->
                editPrefs[KEY_FAILED_ATTEMPTS] = nextAttempts
                editPrefs[KEY_LOCKOUT_UNTIL_MS] = 0L
            }
            PinVerifyResult.Invalid(attemptsRemaining = MAX_FAILED_ATTEMPTS - nextAttempts)
        }
    }

    private fun derivePinHash(pin: String, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(pin.toCharArray(), salt, PBKDF2_ITERATIONS, KEY_LENGTH_BITS)
        return try {
            SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }
}
