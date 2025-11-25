package week11.st421007.finalproject.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import week11.st421007.finalproject.data.AuthRepository
import week11.st421007.finalproject.util.UiState

class AuthViewModel : ViewModel() {
    private val authRepository = AuthRepository()

    private val _authState = MutableStateFlow<UiState<String>>(UiState.Idle)
    val authState: StateFlow<UiState<String>> = _authState.asStateFlow()

    private val _isAuthenticated = MutableStateFlow(authRepository.isUserAuthenticated)
    val isAuthenticated: StateFlow<Boolean> = _isAuthenticated.asStateFlow()

    val currentUserId: String?
        get() = authRepository.currentUserId

    private val _passwordResetState = MutableStateFlow<UiState<String>>(UiState.Idle)
    val passwordResetState: StateFlow<UiState<String>> = _passwordResetState.asStateFlow()

    init {
        checkAuthStatus()
    }

    fun checkAuthStatus() {
        _isAuthenticated.value = authRepository.isUserAuthenticated
    }

    fun signUp(email: String, password: String, confirmPassword: String) {
        if (email.isBlank() || password.isBlank() || confirmPassword.isBlank()) {
            _authState.value = UiState.Error("All fields are required")
            return
        }

        if (!authRepository.isValidEmail(email)) {
            _authState.value = UiState.Error("Invalid email format")
            return
        }

        if (!authRepository.isValidPassword(password)) {
            _authState.value = UiState.Error("Password must be at least 6 characters")
            return
        }

        if (password != confirmPassword) {
            _authState.value = UiState.Error("Passwords do not match")
            return
        }

        _authState.value = UiState.Loading
        viewModelScope.launch {
            authRepository.signUp(email, password).collect { result ->
                result.fold(
                    onSuccess = { user ->
                        _authState.value = UiState.Success(user.uid)
                        _isAuthenticated.value = true
                    },
                    onFailure = { exception ->
                        _authState.value = UiState.Error(
                            exception.message ?: "Sign up failed"
                        )
                    }
                )
            }
        }
    }

    fun signIn(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _authState.value = UiState.Error("Email and password are required")
            return
        }

        if (!authRepository.isValidEmail(email)) {
            _authState.value = UiState.Error("Invalid email format")
            return
        }

        _authState.value = UiState.Loading
        viewModelScope.launch {
            authRepository.signIn(email, password).collect { result ->
                result.fold(
                    onSuccess = { user ->
                        _authState.value = UiState.Success(user.uid)
                        _isAuthenticated.value = true
                    },
                    onFailure = { exception ->
                        _authState.value = UiState.Error(
                            exception.message ?: "Sign in failed"
                        )
                    }
                )
            }
        }
    }

    fun signOut() {
        authRepository.signOut()
        _isAuthenticated.value = false
        _authState.value = UiState.Idle
    }

    fun resetAuthState() {
        _authState.value = UiState.Idle
    }

    fun sendPasswordResetEmail(email: String) {
        if (email.isBlank()) {
            _passwordResetState.value = UiState.Error("Email is required")
            return
        }

        if (!authRepository.isValidEmail(email)) {
            _passwordResetState.value = UiState.Error("Invalid email format")
            return
        }

        _passwordResetState.value = UiState.Loading
        viewModelScope.launch {
            authRepository.sendPasswordResetEmail(email).collect { result ->
                result.fold(
                    onSuccess = {
                        _passwordResetState.value = UiState.Success("Password reset email sent")
                    },
                    onFailure = { exception ->
                        _passwordResetState.value = UiState.Error(
                            exception.message ?: "Failed to send reset email"
                        )
                    }
                )
            }
        }
    }

    fun resetPasswordResetState() {
        _passwordResetState.value = UiState.Idle
    }
}