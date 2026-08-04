package com.venkatbhoogarbh.krypta.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.venkatbhoogarbh.krypta.util.PasswordStrength
import com.venkatbhoogarbh.krypta.util.PasswordUtils
import com.venkatbhoogarbh.krypta.util.SecurityUtils
import com.venkatbhoogarbh.krypta.util.SessionManager
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
        return SecurityUtils.isMasterPasswordSet(getApplication())
    }

    fun setupMasterPassword(password: String) {
        val strength = PasswordUtils.evaluateStrength(password)
        if (strength == PasswordStrength.WEAK) {
            _authState.value = AuthState.Error("Password is too weak. Please use a stronger password.")
            return
        }

        viewModelScope.launch {
            _authState.value = AuthState.Loading
            SecurityUtils.setMasterPassword(getApplication(), password.toCharArray())
            SessionManager.authenticate()
            _authState.value = AuthState.Success
        }
    }

    fun login(password: String) {
        if (SessionManager.cooldownTimeLeft.value > 0) {
            _authState.value = AuthState.Error("Too many failed attempts. Please wait.")
            return
        }

        viewModelScope.launch {
            _authState.value = AuthState.Loading
            val isValid = SecurityUtils.verifyMasterPassword(getApplication(), password.toCharArray())
            
            if (isValid) {
                SessionManager.authenticate()
                _authState.value = AuthState.Success
            } else {
                SessionManager.reportFailedAttempt()
                if (SessionManager.failedAttempts.value >= 3) {
                    startCooldown()
                }
                _authState.value = AuthState.Error("Incorrect master password.")
            }
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
