package com.venkatbhoogarbh.krypta.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.venkatbhoogarbh.krypta.util.KeyManager
import com.venkatbhoogarbh.krypta.util.MigrationManager
import com.venkatbhoogarbh.krypta.util.PasswordStrength
import com.venkatbhoogarbh.krypta.util.PasswordUtils
import com.venkatbhoogarbh.krypta.util.SecurityUtils
import com.venkatbhoogarbh.krypta.util.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AuthViewModel(application: Application) : AndroidViewModel(application) {
    
    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState = _authState.asStateFlow()

    private var cooldownJob: Job? = null

    sealed class AuthState {
        object Idle : AuthState()
        object Loading : AuthState()
        object Success : AuthState()
        data class Error(val message: String) : AuthState()
    }

    fun isMasterPasswordSet(): Boolean {
        return SecurityUtils.isMasterPasswordSet(getApplication<Application>().applicationContext)
    }

    fun setupMasterPassword(password: String) {
        val strength = PasswordUtils.evaluateStrength(password)
        if (strength == PasswordStrength.WEAK) {
            _authState.value = AuthState.Error("Password is too weak. Please use a stronger password.")
            return
        }

        viewModelScope.launch {
            _authState.value = AuthState.Loading
            
            val salt = SecurityUtils.generateSalt()
            val pdk = SecurityUtils.derivePdk(password, salt)
            
            try {
                val mdk = KeyManager.generateRandomKey()
                val ddk = KeyManager.generateRandomKey()
                val sdk = KeyManager.generateRandomKey()
                
                SecurityUtils.saveKeyBundle(getApplication(), mdk, ddk, sdk, pdk)
                
                // Also save the salt for Argon2
                val prefs = SecurityUtils.getEncryptedPrefs(getApplication())
                prefs.edit().apply {
                    putString("password_salt", android.util.Base64.encodeToString(salt, android.util.Base64.NO_WRAP))
                    putInt("algorithm_version", 2)
                    putBoolean("master_password_set", true)
                    apply()
                }

                SessionManager.authenticate(mdk, ddk, sdk)
                _authState.value = AuthState.Success
            } finally {
                pdk.fill(0)
            }
        }
    }

    fun login(password: String) {
        if (SessionManager.cooldownTimeLeft.value > 0) {
            _authState.value = AuthState.Error("Too many failed attempts. Please wait.")
            return
        }

        viewModelScope.launch {
            _authState.value = AuthState.Loading
            
            val prefs = SecurityUtils.getEncryptedPrefs(getApplication())
            val version = prefs.getInt("algorithm_version", 1)
            
            if (version == 1) {
                handleMigration(password)
                return@launch
            }

            val saltBase64 = prefs.getString("password_salt", null)
            if (saltBase64 == null) {
                _authState.value = AuthState.Error("Security configuration missing.")
                return@launch
            }
            
            val salt = android.util.Base64.decode(saltBase64, android.util.Base64.NO_WRAP)
            val pdk = SecurityUtils.derivePdk(password, salt)
            
            try {
                val bundle = SecurityUtils.getActiveKeyBundle(getApplication())
                if (bundle == null) {
                    _authState.value = AuthState.Error("Vault key bundle missing.")
                    return@launch
                }

                val keys = SecurityUtils.unwrapDataKeys(bundle, pdk)
                
                if (keys != null) {
                    SessionManager.authenticate(
                        keys["mdk"]!!,
                        keys["ddk"]!!,
                        keys["sdk"]!!
                    )
                    _authState.value = AuthState.Success
                } else {
                    SessionManager.reportFailedAttempt()
                    if (SessionManager.failedAttempts.value >= 3) {
                        startCooldown()
                    }
                    _authState.value = AuthState.Error("Incorrect master password.")
                }
            } finally {
                pdk.fill(0)
            }
        }
    }

    private suspend fun handleMigration(password: String) {
        val prefs = getApplication<Application>().getSharedPreferences("krypta_secure_prefs", android.content.Context.MODE_PRIVATE)
        val saltBase64 = prefs.getString("password_salt", null)
        val verifierBase64 = prefs.getString("password_verifier", null)
        
        if (saltBase64 == null || verifierBase64 == null) {
            _authState.value = AuthState.Error("Legacy security configuration missing.")
            return
        }

        val salt = android.util.Base64.decode(saltBase64, android.util.Base64.NO_WRAP)
        val storedVerifier = android.util.Base64.decode(verifierBase64, android.util.Base64.NO_WRAP)
        
        val legacyKey = SecurityUtils.deriveLegacyKey(password.toCharArray(), salt)
        
        try {
            if (storedVerifier.contentEquals(legacyKey)) {
                // Valid legacy password, proceed to migration
                var newPdk: ByteArray? = null
                try {
                    val mdk = KeyManager.generateRandomKey()
                    val ddk = KeyManager.generateRandomKey()
                    val sdk = KeyManager.generateRandomKey()
                    
                    val newSalt = SecurityUtils.generateSalt()
                    newPdk = SecurityUtils.derivePdk(password, newSalt)
                    
                    val migrationManager = MigrationManager(getApplication())
                    migrationManager.runMigration(legacyKey, mdk, ddk)
                    
                    SecurityUtils.saveKeyBundle(getApplication(), mdk, ddk, sdk, newPdk)
                    
                    prefs.edit().apply {
                        putString("password_salt", android.util.Base64.encodeToString(newSalt, android.util.Base64.NO_WRAP))
                        putInt("algorithm_version", 2)
                        remove("password_verifier") // Verifier is now in the bundle
                        apply()
                    }
                    
                    SessionManager.authenticate(mdk, ddk, sdk)
                    _authState.value = AuthState.Success
                } catch (e: Exception) {
                    _authState.value = AuthState.Error("Migration failed: ${e.message}")
                } finally {
                    newPdk?.fill(0)
                }
            } else {
                SessionManager.reportFailedAttempt()
                _authState.value = AuthState.Error("Incorrect master password.")
            }
        } finally {
            legacyKey.fill(0)
        }
    }

    private fun startCooldown() {
        cooldownJob?.cancel()
        cooldownJob = viewModelScope.launch {
            var timeLeft = 30000L
            while (timeLeft > 0) {
                SessionManager.setCooldown(timeLeft)
                delay(1000)
                timeLeft -= 1000
            }
            SessionManager.setCooldown(0)
        }
    }

    fun resetState() {
        _authState.value = AuthState.Idle
    }
}
