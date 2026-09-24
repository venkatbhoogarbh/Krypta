package com.venkatbhoogarbh.krypta.util

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import org.json.JSONObject
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.Mac
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Handles Android Keystore interactions and Nested Envelope Encryption.
 */
object KeyManager {
    private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
    private const val MASTER_KEY_ALIAS = "krypta_master_root"
    private const val VERIFIER_CONSTANT = "KRYPTA_VERIFY_V3"

    private val keyStore: KeyStore by lazy {
        KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }
    }

    /**
     * Ensures the Master Wrapping Key exists in Keystore.
     */
    fun getOrCreateMasterWrappingKey(): SecretKey {
        if (!keyStore.containsAlias(MASTER_KEY_ALIAS)) {
            val keyGenerator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES,
                KEYSTORE_PROVIDER
            )
            val spec = KeyGenParameterSpec.Builder(
                MASTER_KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .setUserAuthenticationRequired(false)
                .build()
            keyGenerator.init(spec)
            keyGenerator.generateKey()
        }
        return keyStore.getKey(MASTER_KEY_ALIAS, null) as SecretKey
    }

    /**
     * Performs Nested Envelope Encryption: MWK(PDK(DataKey)).
     */
    fun wrapKey(dataKey: ByteArray, pdk: ByteArray): JSONObject {
        val mwk = getOrCreateMasterWrappingKey()
        val random = SecureRandom()

        // 1. Inner Layer: Password Protection
        val innerIv = ByteArray(12).apply { random.nextBytes(this) }
        val innerCipher = Cipher.getInstance("AES/GCM/NoPadding")
        innerCipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(pdk, "AES"), GCMParameterSpec(128, innerIv))
        val innerPayload = innerCipher.doFinal(dataKey)

        try {
            // 2. Outer Layer: Hardware Protection
            val outerCipher = Cipher.getInstance("AES/GCM/NoPadding")
            
            outerCipher.init(Cipher.ENCRYPT_MODE, mwk)
            val outerPayload = outerCipher.doFinal(innerPayload)
            val outerIv = outerCipher.iv

            return JSONObject().apply {
                put("outer_iv", Base64.encodeToString(outerIv, Base64.NO_WRAP))
                put("inner_iv", Base64.encodeToString(innerIv, Base64.NO_WRAP))
                put("payload", Base64.encodeToString(outerPayload, Base64.NO_WRAP))
            }
        } finally {
            innerPayload.fill(0)
        }
    }

    /**
     * Performs Nested Envelope Decryption.
     */
    fun unwrapKey(envelope: JSONObject, pdk: ByteArray): ByteArray? {
        val mwk = getOrCreateMasterWrappingKey()

        try {
            val outerIv = Base64.decode(envelope.getString("outer_iv"), Base64.NO_WRAP)
            val innerIv = Base64.decode(envelope.getString("inner_iv"), Base64.NO_WRAP)
            val outerPayload = Base64.decode(envelope.getString("payload"), Base64.NO_WRAP)

            // 1. Decrypt Outer Layer (Hardware)
            val outerCipher = Cipher.getInstance("AES/GCM/NoPadding")
            outerCipher.init(Cipher.DECRYPT_MODE, mwk, GCMParameterSpec(128, outerIv))
            val innerPayload = outerCipher.doFinal(outerPayload)

            try {
                // 2. Decrypt Inner Layer (Password)
                val innerCipher = Cipher.getInstance("AES/GCM/NoPadding")
                innerCipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(pdk, "AES"), GCMParameterSpec(128, innerIv))
                return innerCipher.doFinal(innerPayload)
            } finally {
                innerPayload.fill(0)
            }
        } catch (e: Exception) {
            return null
        }
    }

    /**
     * Generates a dedicated HMAC-SHA256 verifier for a key.
     */
    fun generateVerifier(key: ByteArray): String {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(key, "HmacSHA256"))
        val verifierBytes = mac.doFinal(VERIFIER_CONSTANT.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(verifierBytes, Base64.NO_WRAP)
    }

    /**
     * Verifies a candidate key using constant-time comparison.
     */
    fun verifyKey(candidateKey: ByteArray, storedVerifier: String): Boolean {
        val calculated = generateVerifier(candidateKey)
        return MessageDigestUtils.constantTimeAreEqual(calculated, storedVerifier)
    }

    /**
     * Generates a new random 256-bit key.
     */
    fun generateRandomKey(): ByteArray {
        val key = ByteArray(32)
        SecureRandom().nextBytes(key)
        return key
    }
}

object MessageDigestUtils {
    fun constantTimeAreEqual(a: String, b: String): Boolean {
        val aBytes = a.toByteArray()
        val bBytes = b.toByteArray()
        if (aBytes.size != bBytes.size) return false
        var result = 0
        for (i in aBytes.indices) {
            result = result or (aBytes[i].toInt() xor bBytes[i].toInt())
        }
        return result == 0
    }
}
