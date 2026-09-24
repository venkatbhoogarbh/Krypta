package com.venkatbhoogarbh.krypta.util

import android.content.Context
import android.util.Base64
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.lambdapioneer.argon2kt.Argon2Kt
import com.lambdapioneer.argon2kt.Argon2Mode
import org.json.JSONObject
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

    // Argon2id Parameters
    private const val ARGON2_MEMORY = 65536
    private const val ARGON2_ITERATIONS = 3
    private const val ARGON2_PARALLELISM = 1
    private const val ARGON2_HASH_LENGTH = 32

    private const val PBKDF2_ALGORITHM = "PBKDF2WithHmacSHA256"
    private const val ALGORITHM_VERSION = 2 // Phase 7.1 target version
    private const val ITERATION_COUNT = 210000
    private const val KEY_LENGTH = 256
    private const val SALT_LENGTH = 32

    private const val AES_ALGORITHM = "AES/GCM/NoPadding"
    private const val IV_LENGTH = 12
    private const val GCM_TAG_LENGTH = 128

    private val argon2Kt = Argon2Kt()

    fun getEncryptedPrefs(context: Context) = EncryptedSharedPreferences.create(
        context,
        PREFS_FILE,
        MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun isMasterPasswordSet(context: Context): Boolean {
        return getEncryptedPrefs(context).getBoolean(KEY_IS_SET, false)
    }

    /**
     * Authenticated Nested Unwrapping Logic.
     */
    fun unwrapDataKeys(envelopeBundle: JSONObject, pdk: ByteArray): Map<String, ByteArray>? {
        var mdk: ByteArray? = null
        var ddk: ByteArray? = null
        var sdk: ByteArray? = null
        var success = false
        try {
            val envelopes = envelopeBundle.getJSONObject("envelopes")
            
            mdk = KeyManager.unwrapKey(envelopes.getJSONObject("mdk"), pdk)
            if (mdk == null) return null
            
            ddk = KeyManager.unwrapKey(envelopes.getJSONObject("ddk"), pdk)
            if (ddk == null) return null
            
            sdk = KeyManager.unwrapKey(envelopes.getJSONObject("sdk"), pdk)
            if (sdk == null) return null

            // Verify MDK
            val storedVerifier = envelopeBundle.getString("verifier")
            if (!KeyManager.verifyKey(mdk, storedVerifier)) {
                return null
            }

            success = true
            return mapOf("mdk" to mdk, "ddk" to ddk, "sdk" to sdk)
        } catch (e: Exception) {
            return null
        } finally {
            if (!success) {
                mdk?.fill(0)
                ddk?.fill(0)
                sdk?.fill(0)
            }
        }
    }

    /**
     * Secure Key Storage (Two-Slot System).
     */
    fun saveKeyBundle(context: Context, mdk: ByteArray, ddk: ByteArray, sdk: ByteArray, pdk: ByteArray) {
        val prefs = context.getSharedPreferences("krypta_key_slots", Context.MODE_PRIVATE)
        val currentGen = prefs.getInt("active_generation", 0)
        val nextGen = currentGen + 1
        
        val mdkEnvelope = KeyManager.wrapKey(mdk, pdk)
        val ddkEnvelope = KeyManager.wrapKey(ddk, pdk)
        val sdkEnvelope = KeyManager.wrapKey(sdk, pdk)
        val verifier = KeyManager.generateVerifier(mdk)

        val bundle = JSONObject().apply {
            put("version", 3)
            put("generation", nextGen)
            put("envelopes", JSONObject().apply {
                put("mdk", mdkEnvelope)
                put("ddk", ddkEnvelope)
                put("sdk", sdkEnvelope)
            })
            put("verifier", verifier)
        }

        val slot = if (prefs.getString("active_slot", "a") == "a") "b" else "a"
        val slotKey = "slot_$slot"

        prefs.edit().apply {
            putString(slotKey, bundle.toString())
            putInt("active_generation", nextGen)
            putString("active_slot", slot)
            apply()
        }
    }

    fun getActiveKeyBundle(context: Context): JSONObject? {
        val prefs = context.getSharedPreferences("krypta_key_slots", Context.MODE_PRIVATE)
        
        fun parseAndValidateSlot(slotName: String): JSONObject? {
            try {
                val bundleStr = prefs.getString("slot_$slotName", null) ?: return null
                val json = JSONObject(bundleStr)
                
                // Validate structural fields
                if (!json.has("version") || !json.has("generation") || !json.has("envelopes") || !json.has("verifier")) {
                    return null
                }
                
                val envelopes = json.getJSONObject("envelopes")
                if (!envelopes.has("mdk") || !envelopes.has("ddk") || !envelopes.has("sdk")) {
                    return null
                }

                return json
            } catch (e: Exception) {
                return null
            }
        }

        val bundleA = parseAndValidateSlot("a")
        val bundleB = parseAndValidateSlot("b")

        val genA = bundleA?.optInt("generation", -1) ?: -1
        val genB = bundleB?.optInt("generation", -1) ?: -1

        return when {
            bundleA != null && bundleB != null -> if (genA >= genB) bundleA else bundleB
            bundleA != null -> bundleA
            bundleB != null -> bundleB
            else -> null
        }
    }

    /**
     * Legacy PBKDF2 derivation (only for migration).
     */
    fun deriveLegacyKey(password: CharArray, salt: ByteArray, iterations: Int = ITERATION_COUNT): ByteArray {
        val spec = PBEKeySpec(password, salt, iterations, KEY_LENGTH)
        val factory = SecretKeyFactory.getInstance(PBKDF2_ALGORITHM)
        return factory.generateSecret(spec).encoded
    }

    /**
     * Argon2id derivation for production.
     */
    fun derivePdk(password: String, salt: ByteArray): ByteArray {
        val result = argon2Kt.hash(
            mode = Argon2Mode.ARGON2_ID,
            password = password.toByteArray(StandardCharsets.UTF_8),
            salt = salt,
            tCostInIterations = ARGON2_ITERATIONS,
            mCostInKibibyte = ARGON2_MEMORY,
            parallelism = ARGON2_PARALLELISM,
            hashLengthInBytes = ARGON2_HASH_LENGTH
        )
        return result.rawHashAsByteArray()
    }

    fun generateSalt(): ByteArray {
        val random = SecureRandom()
        val salt = ByteArray(SALT_LENGTH)
        random.nextBytes(salt)
        return salt
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
        
        try {
            return String(plaintext, StandardCharsets.UTF_8)
        } finally {
            plaintext.fill(0)
        }
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
}
