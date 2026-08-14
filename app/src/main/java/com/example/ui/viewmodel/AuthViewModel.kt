package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.repository.AuthRepository
import com.example.data.repository.AuthState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AuthViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    val authState: StateFlow<AuthState> = authRepository.authState

    private val _emailInput = MutableStateFlow("")
    val emailInput: StateFlow<String> = _emailInput.asStateFlow()

    private val _passwordInput = MutableStateFlow("")
    val passwordInput: StateFlow<String> = _passwordInput.asStateFlow()

    private val _displayNameInput = MutableStateFlow("")
    val displayNameInput: StateFlow<String> = _displayNameInput.asStateFlow()

    private val _isRegisterMode = MutableStateFlow(false)
    val isRegisterMode: StateFlow<Boolean> = _isRegisterMode.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun onEmailChange(value: String) {
        _emailInput.value = value
        _errorMessage.value = null
    }

    fun onPasswordChange(value: String) {
        _passwordInput.value = value
        _errorMessage.value = null
    }

    fun onDisplayNameChange(value: String) {
        _displayNameInput.value = value
    }

    fun setRegisterMode(isRegister: Boolean) {
        _isRegisterMode.value = isRegister
        _errorMessage.value = null
    }

    fun submitAuth() {
        val email = _emailInput.value.trim()
        val pass = _passwordInput.value.trim()

        if (email.isEmpty() || !email.contains("@")) {
            _errorMessage.value = "يرجى إدخال بريد إلكتروني صحيح"
            return
        }

        if (pass.length < 6) {
            _errorMessage.value = "يجب أن تتكون كلمة المرور من 6 أحرف على الأقل"
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            if (_isRegisterMode.value) {
                val name = _displayNameInput.value.trim()
                val result = authRepository.registerWithEmail(email, pass, name)
                if (result.isFailure) {
                    _errorMessage.value = result.exceptionOrNull()?.localizedMessage ?: "حدث خطأ أثناء التسجيل"
                }
            } else {
                val result = authRepository.loginWithEmail(email, pass)
                if (result.isFailure) {
                    _errorMessage.value = result.exceptionOrNull()?.localizedMessage ?: "حدث خطأ أثناء تسجيل الدخول"
                }
            }
            _isLoading.value = false
        }
    }

    fun simulateAdminApproval(approved: Boolean) {
        viewModelScope.launch {
            authRepository.simulateVipActivationLocally(approved)
        }
    }

    fun signOut() {
        authRepository.signOut()
        _emailInput.value = ""
        _passwordInput.value = ""
        _displayNameInput.value = ""
        _errorMessage.value = null
    }

    fun clearError() {
        _errorMessage.value = null
    }
}
