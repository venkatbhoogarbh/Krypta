package com.venkatbhoogarbh.krypta

import com.venkatbhoogarbh.krypta.util.KeyManager
import com.venkatbhoogarbh.krypta.util.SecurityUtils
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for cryptographic logic that doesn't depend on the Android Keystore hardware.
 * Note: Keystore tests must be run on a device/emulator.
 */
class CryptoLogicTest {

    @Test
    fun testPasswordVerification() {
        val mdk = KeyManager.generateRandomKey()
        val verifier = KeyManager.generateVerifier(mdk)
        
        assertTrue(KeyManager.verifyKey(mdk, verifier))
        
        val wrongMdk = KeyManager.generateRandomKey()
        assertFalse(KeyManager.verifyKey(wrongMdk, verifier))
    }

    @Test
    fun testArgon2Derivation() {
        val password = "test_password"
        val salt = SecurityUtils.generateSalt()
        
        val pdk1 = SecurityUtils.derivePdk(password, salt)
        val pdk2 = SecurityUtils.derivePdk(password, salt)
        
        assertArrayEquals(pdk1, pdk2)
        assertEquals(32, pdk1.size)
    }
}
