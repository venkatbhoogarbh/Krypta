package com.venkatbhoogarbh.krypta.util

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

object SessionManager {
    private val _isAuthenticated = MutableStateFlow(false)
    val isAuthenticated = _isAuthenticated.asStateFlow()

    private var _mdk: ByteArray? = null
    val mdk: ByteArray? get() = _mdk

    private var _ddk: ByteArray? = null
    val ddk: ByteArray? get() = _ddk

    private var _sdk: ByteArray? = null
    val sdk: ByteArray? get() = _sdk

    private val _failedAttempts = MutableStateFlow(0)
    val failedAttempts = _failedAttempts.asStateFlow()

    private val _cooldownTimeLeft = MutableStateFlow(0L)
    val cooldownTimeLeft = _cooldownTimeLeft.asStateFlow()

    fun authenticate(mdk: ByteArray, ddk: ByteArray, sdk: ByteArray) {
        _mdk = mdk
        _ddk = ddk
        _sdk = sdk
        _isAuthenticated.value = true
        _failedAttempts.value = 0
    }

    fun lock() {
        _mdk?.fill(0)
        _ddk?.fill(0)
        _sdk?.fill(0)
        _mdk = null
        _ddk = null
        _sdk = null
        _isAuthenticated.value = false
    }

    fun reportFailedAttempt() {
        _failedAttempts.value += 1
    }

    fun setCooldown(millis: Long) {
        _cooldownTimeLeft.value = millis
    }
}
