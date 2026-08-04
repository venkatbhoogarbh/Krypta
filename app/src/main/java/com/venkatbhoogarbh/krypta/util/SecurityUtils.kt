package com.venkatbhoogarbh.krypta.util

import android.content.Context
import android.os.Build
import android.util.Base64
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.io.InputStream
import java.io.OutputStream
import java.nio.charset.StandardCharsets
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.CipherInputStream
import javax.crypto.CipherOutputStream
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

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

    private const val AES_ALGORITHM = "AES/GCM/NoPadding"
    private const val IV_LENGTH = 12
    private const val GCM_TAG_LENGTH = 128

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

    fun setMasterPassword(context: Context, password: CharArray): ByteArray {
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
        return verifier
    }

    fun verifyMasterPassword(context: Context, password: CharArray): ByteArray? {
        val prefs = getEncryptedPrefs(context)
        val storedVerifierBase64 = prefs.getString(KEY_VERIFIER, null) ?: return null
        val storedSaltBase64 = prefs.getString(KEY_SALT, null) ?: return null
        val iterations = prefs.getInt(KEY_ITERATIONS, ITERATION_COUNT)

        val storedVerifier = Base64.decode(storedVerifierBase64, Base64.NO_WRAP)
        val storedSalt = Base64.decode(storedSaltBase64, Base64.NO_WRAP)

        val currentVerifier = deriveKey(password, storedSalt, iterations)
        
        return if (storedVerifier.contentEquals(currentVerifier)) currentVerifier else null
    }

    // --- AES-GCM Encryption Logic ---

    fun encrypt(plaintext: String, key: ByteArray): String {
        val random = SecureRandom()
        val iv = ByteArray(IV_LENGTH)
        random.nextBytes(iv)

        val cipher = Cipher.getInstance(AES_ALGORITHM)
        val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
        val secretKey = SecretKeySpec(key, "AES")
        
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, spec)
        val ciphertext = cipher.doFinal(plaintext.toByteArray(StandardCharsets.UTF_8))
        
        val combined = ByteArray(iv.size + ciphertext.size)
        System.arraycopy(iv, 0, combined, 0, iv.size)
        System.arraycopy(ciphertext, 0, combined, iv.size, ciphertext.size)
        
        return Base64.encodeToString(combined, Base64.NO_WRAP)
    }

    fun decrypt(base64Ciphertext: String, key: ByteArray): String {
        val combined = Base64.decode(base64Ciphertext, Base64.NO_WRAP)
        
        val iv = ByteArray(IV_LENGTH)
        System.arraycopy(combined, 0, iv, 0, iv.size)
        
        val ciphertext = ByteArray(combined.size - iv.size)
        System.arraycopy(combined, iv.size, ciphertext, 0, ciphertext.size)

        val cipher = Cipher.getInstance(AES_ALGORITHM)
        val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
        val secretKey = SecretKeySpec(key, "AES")
        
        cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)
        val plaintext = cipher.doFinal(ciphertext)
        
        return String(plaintext, StandardCharsets.UTF_8)
    }

    fun getEncryptStream(outputStream: OutputStream, key: ByteArray): OutputStream {
        val random = SecureRandom()
        val iv = ByteArray(IV_LENGTH)
        random.nextBytes(iv)
        
        // Write IV to the start of the stream
        outputStream.write(iv)
        
        val cipher = Cipher.getInstance(AES_ALGORITHM)
        val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
        val secretKey = SecretKeySpec(key, "AES")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, spec)
        
        return CipherOutputStream(outputStream, cipher)
    }

    fun getDecryptStream(inputStream: InputStream, key: ByteArray): InputStream {
        val iv = ByteArray(IV_LENGTH)
        var bytesRead = 0
        while (bytesRead < IV_LENGTH) {
            val n = inputStream.read(iv, bytesRead, IV_LENGTH - bytesRead)
            if (n == -1) throw Exception("Invalid encrypted stream")
            bytesRead += n
        }

        val cipher = Cipher.getInstance(AES_ALGORITHM)
        val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
        val secretKey = SecretKeySpec(key, "AES")
        cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)
        
        return CipherInputStream(inputStream, cipher)
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
