package com.venkatbhoogarbh.krypta

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.venkatbhoogarbh.krypta.util.KeyManager
import com.venkatbhoogarbh.krypta.util.SecurityUtils
import org.json.JSONObject
import org.junit.Before
import org.junit.After
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CryptoMigrationTest {

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        context.getSharedPreferences("krypta_key_slots", android.content.Context.MODE_PRIVATE)
            .edit().clear().commit()
    }

    @After
    fun tearDown() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        context.getSharedPreferences("krypta_key_slots", android.content.Context.MODE_PRIVATE)
            .edit().clear().commit()
    }

    @Test
    fun testNestedEnvelopeEncryption() {
        val pdk = ByteArray(32) { 1.toByte() }
        val mdk = ByteArray(32) { 2.toByte() }

        // Wrap
        val envelope = KeyManager.wrapKey(mdk, pdk)
        assertNotNull(envelope)
        
        // Unwrap
        val unwrappedMdk = KeyManager.unwrapKey(envelope, pdk)
        assertArrayEquals(mdk, unwrappedMdk)
    }

    @Test
    fun testPasswordVerification() {
        val mdk = KeyManager.generateRandomKey()
        val verifier = KeyManager.generateVerifier(mdk)
        
        assertTrue(KeyManager.verifyKey(mdk, verifier))
        
        val wrongMdk = KeyManager.generateRandomKey()
        assertFalse(KeyManager.verifyKey(wrongMdk, verifier))
    }

    @Test
    fun testKeyBundleSerialization() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val pdk = SecurityUtils.generateSalt() // Just 32 bytes
        val mdk = KeyManager.generateRandomKey()
        val ddk = KeyManager.generateRandomKey()
        val sdk = KeyManager.generateRandomKey()

        SecurityUtils.saveKeyBundle(context, mdk, ddk, sdk, pdk)
        
        val bundle = SecurityUtils.getActiveKeyBundle(context)
        assertNotNull(bundle)
        
        val keys = SecurityUtils.unwrapDataKeys(bundle!!, pdk)
        assertNotNull(keys)
        assertArrayEquals(mdk, keys!!["mdk"])
        assertArrayEquals(ddk, keys!!["ddk"])
        assertArrayEquals(sdk, keys!!["sdk"])
    }

    @Test
    fun testTwoSlotRecoveryScenarios() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val prefs = context.getSharedPreferences("krypta_key_slots", android.content.Context.MODE_PRIVATE)
        
        val validObjA = JSONObject().apply {
            put("version", 3)
            put("generation", 10)
            put("envelopes", JSONObject().apply {
                put("mdk", JSONObject())
                put("ddk", JSONObject())
                put("sdk", JSONObject())
            })
            put("verifier", "verifier_a")
        }

        val validObjB = JSONObject().apply {
            put("version", 3)
            put("generation", 15)
            put("envelopes", JSONObject().apply {
                put("mdk", JSONObject())
                put("ddk", JSONObject())
                put("sdk", JSONObject())
            })
            put("verifier", "verifier_b")
        }

        // Scenario a: Both slots valid -> highest generation selected (slot_b generation 15)
        prefs.edit().putString("slot_a", validObjA.toString()).putString("slot_b", validObjB.toString()).commit()
        var recovered = SecurityUtils.getActiveKeyBundle(context)
        assertNotNull(recovered)
        assertEquals(15, recovered!!.getInt("generation"))

        // Scenario b: Slot A newer than Slot B (20 vs 15) -> Slot A selected
        validObjA.put("generation", 20)
        prefs.edit().putString("slot_a", validObjA.toString()).commit()
        recovered = SecurityUtils.getActiveKeyBundle(context)
        assertNotNull(recovered)
        assertEquals(20, recovered!!.getInt("generation"))

        // Scenario c: Slot A malformed + Slot B valid (15) -> Slot B recovered
        prefs.edit().putString("slot_a", "{ malformed json: ").commit()
        recovered = SecurityUtils.getActiveKeyBundle(context)
        assertNotNull(recovered)
        assertEquals(15, recovered!!.getInt("generation"))

        // Scenario d: Slot A missing + Slot B valid (15) -> Slot B recovered
        prefs.edit().remove("slot_a").commit()
        recovered = SecurityUtils.getActiveKeyBundle(context)
        assertNotNull(recovered)
        assertEquals(15, recovered!!.getInt("generation"))

        // Scenario e: Both slots invalid -> null
        prefs.edit().putString("slot_b", "{}").commit() // missing envelopes, verifier, etc.
        recovered = SecurityUtils.getActiveKeyBundle(context)
        assertNull(recovered)

        // Reset to valid states for next scenarios
        validObjA.put("generation", 100)
        validObjB.put("generation", 50)
        prefs.edit().putString("slot_a", validObjA.toString()).putString("slot_b", validObjB.toString()).commit()

        // Scenario f: active_slot missing + valid slot exists -> highest valid slot recovered (100)
        prefs.edit().remove("active_slot").commit()
        recovered = SecurityUtils.getActiveKeyBundle(context)
        assertNotNull(recovered)
        assertEquals(100, recovered!!.getInt("generation"))

        // Scenario g: active_slot points to the WRONG slot (b) + both slots valid -> highest generation selected (100)
        prefs.edit().putString("active_slot", "b").commit()
        recovered = SecurityUtils.getActiveKeyBundle(context)
        assertNotNull(recovered)
        assertEquals(100, recovered!!.getInt("generation"))

        // Scenario h: active_slot contains an invalid value -> recovery still works (100)
        prefs.edit().putString("active_slot", "garbage").commit()
        recovered = SecurityUtils.getActiveKeyBundle(context)
        assertNotNull(recovered)
        assertEquals(100, recovered!!.getInt("generation"))
    }
}
