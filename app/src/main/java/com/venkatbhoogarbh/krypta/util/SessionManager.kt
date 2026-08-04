package com.venkatbhoogarbh.krypta.util

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

object SessionManager {
    private val _isAuthenticated = MutableStateFlow(false)
    val isAuthenticated = _isAuthenticated.asStateFlow()

    private val _failedAttempts = MutableStateFlow(0)
    val failedAttempts = _failedAttempts.asStateFlow()

    private val _cooldownTimeLeft = MutableStateFlow(0L)
    val cooldownTimeLeft = _cooldownTimeLeft.asStateFlow()

    fun authenticate() {
        _isAuthenticated.value = true
        _failedAttempts.value = 0
    }

    fun lock() {
        _isAuthenticated.value = false
    }

    fun reportFailedAttempt() {
        _failedAttempts.value += 1
    }

    fun setCooldown(millis: Long) {
        _cooldownTimeLeft.value = millis
    }
}
