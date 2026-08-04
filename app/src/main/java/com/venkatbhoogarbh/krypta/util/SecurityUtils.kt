package com.venkatbhoogarbh.krypta.util

import android.content.Context
import android.os.Build
import android.util.Base64
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

object SecurityUtils {
    private const val PREFS_FILE = "krypta_secure_prefs"
    private const val KEY_VERIFIER = "password_verifier"
    private const val KEY_SALT = "password_salt"
    private const val KEY_ITERATIONS = "iteration_count"
    private const val KEY_VERSION = "algorithm_version"
    private const val KEY_IS_SET = "master_password_set"

    private const val PBKDF2_ALGORITHM = "PBKDF2WithHmacSHA256"
    private const val ALGORITHM_VERSION = 1
    private const val ITERATION_COUNT = 210000
    private const val KEY_LENGTH = 256
    private const val SALT_LENGTH = 32

    private fun getEncryptedPrefs(context: Context) = EncryptedSharedPreferences.create(
        context,
        PREFS_FILE,
        MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun isMasterPasswordSet(context: Context): Boolean {
        return getEncryptedPrefs(context).getBoolean(KEY_IS_SET, false)
    }

    fun setMasterPassword(context: Context, password: CharArray) {
        val salt = generateSalt()
        val verifier = deriveKey(password, salt)
        
        getEncryptedPrefs(context).edit().apply {
            putString(KEY_VERIFIER, Base64.encodeToString(verifier, Base64.NO_WRAP))
            putString(KEY_SALT, Base64.encodeToString(salt, Base64.NO_WRAP))
            putInt(KEY_ITERATIONS, ITERATION_COUNT)
            putInt(KEY_VERSION, ALGORITHM_VERSION)
            putBoolean(KEY_IS_SET, true)
            apply()
        }
    }

    fun verifyMasterPassword(context: Context, password: CharArray): Boolean {
        val prefs = getEncryptedPrefs(context)
        val storedVerifierBase64 = prefs.getString(KEY_VERIFIER, null) ?: return false
        val storedSaltBase64 = prefs.getString(KEY_SALT, null) ?: return false
        val iterations = prefs.getInt(KEY_ITERATIONS, ITERATION_COUNT)

        val storedVerifier = Base64.decode(storedVerifierBase64, Base64.NO_WRAP)
        val storedSalt = Base64.decode(storedSaltBase64, Base64.NO_WRAP)

        val currentVerifier = deriveKey(password, storedSalt, iterations)
        
        return storedVerifier.contentEquals(currentVerifier)
    }

    private fun generateSalt(): ByteArray {
        val random = SecureRandom()
        val salt = ByteArray(SALT_LENGTH)
        random.nextBytes(salt)
        return salt
    }

    private fun deriveKey(password: CharArray, salt: ByteArray, iterations: Int = ITERATION_COUNT): ByteArray {
        val spec = PBEKeySpec(password, salt, iterations, KEY_LENGTH)
        val factory = SecretKeyFactory.getInstance(PBKDF2_ALGORITHM)
        return factory.generateSecret(spec).encoded
    }
}
