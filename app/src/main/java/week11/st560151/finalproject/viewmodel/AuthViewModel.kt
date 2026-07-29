package week11.st560151.finalproject.viewmodel

import android.util.Patterns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import week11.st560151.finalproject.data.repository.AuthRepository
import week11.st560151.finalproject.ui.state.UiState

class AuthViewModel(
    private val repository: AuthRepository = AuthRepository()
) : ViewModel() {

    private val _loginState =
        MutableStateFlow<UiState<Unit>>(UiState.Idle)

    val loginState: StateFlow<UiState<Unit>> =
        _loginState.asStateFlow()

    private val _registerState =
        MutableStateFlow<UiState<Unit>>(UiState.Idle)

    val registerState: StateFlow<UiState<Unit>> =
        _registerState.asStateFlow()

    private val _resetState =
        MutableStateFlow<UiState<Unit>>(UiState.Idle)

    val resetState: StateFlow<UiState<Unit>> =
        _resetState.asStateFlow()

    fun login(
        email: String,
        password: String
    ) {
        val error = validateLogin(email, password)

        if (error != null) {
            _loginState.value = UiState.Error(error)
            return
        }

        viewModelScope.launch {
            _loginState.value = UiState.Loading

            _loginState.value = repository
                .login(email, password)
                .fold(
                    onSuccess = {
                        UiState.Success(Unit)
                    },
                    onFailure = {
                        UiState.Error(
                            it.message ?: "Login failed."
                        )
                    }
                )
        }
    }

    fun register(
        email: String,
        password: String,
        confirmPassword: String
    ) {
        val error = validateRegistration(
            email,
            password,
            confirmPassword
        )

        if (error != null) {
            _registerState.value = UiState.Error(error)
            return
        }

        viewModelScope.launch {
            _registerState.value = UiState.Loading

            _registerState.value = repository
                .register(email, password)
                .fold(
                    onSuccess = {
                        UiState.Success(Unit)
                    },
                    onFailure = {
                        UiState.Error(
                            it.message ?: "Registration failed."
                        )
                    }
                )
        }
    }

    fun sendPasswordReset(
        email: String
    ) {
        if (email.isBlank()) {
            _resetState.value =
                UiState.Error("Email is required.")
            return
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            _resetState.value =
                UiState.Error("Enter a valid email address.")
            return
        }

        viewModelScope.launch {
            _resetState.value = UiState.Loading

            _resetState.value = repository
                .sendPasswordReset(email)
                .fold(
                    onSuccess = {
                        UiState.Success(Unit)
                    },
                    onFailure = {
                        UiState.Error(
                            it.message
                                ?: "Unable to send reset email."
                        )
                    }
                )
        }
    }

    fun logout() {
        repository.logout()
        resetStates()
    }

    fun isSignedIn(): Boolean {
        return repository.getCurrentUser() != null
    }

    fun resetLoginState() {
        _loginState.value = UiState.Idle
    }

    fun resetRegisterState() {
        _registerState.value = UiState.Idle
    }

    fun resetPasswordState() {
        _resetState.value = UiState.Idle
    }

    private fun resetStates() {
        resetLoginState()
        resetRegisterState()
        resetPasswordState()
    }

    private fun validateLogin(
        email: String,
        password: String
    ): String? {
        if (email.isBlank()) {
            return "Email is required."
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            return "Enter a valid email address."
        }

        if (password.isBlank()) {
            return "Password is required."
        }

        return null
    }

    private fun validateRegistration(
        email: String,
        password: String,
        confirmPassword: String
    ): String? {
        if (email.isBlank()) {
            return "Email is required."
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            return "Enter a valid email address."
        }

        if (password.length < 6) {
            return "Password must contain at least 6 characters."
        }

        if (password != confirmPassword) {
            return "Passwords do not match."
        }

        return null
    }
}