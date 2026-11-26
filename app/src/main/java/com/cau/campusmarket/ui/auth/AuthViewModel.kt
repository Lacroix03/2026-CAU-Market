package com.cau.campusmarket.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.cau.campusmarket.data.repository.AuthRepository
import com.cau.campusmarket.domain.validation.Validators
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val emailError: String? = null,
    val passwordError: String? = null,
    val authError: String? = null,
    val infoMessage: String? = null,
    val isPasswordVisible: Boolean = false,
    val isLoading: Boolean = false
)

data class SignUpUiState(
    val email: String = "",
    val nickname: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val emailError: String? = null,
    val nicknameError: String? = null,
    val passwordError: String? = null,
    val confirmError: String? = null,
    val submitError: String? = null,
    val isPasswordVisible: Boolean = false,
    val isConfirmVisible: Boolean = false,
    val isLoading: Boolean = false
)

class AuthViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _loginState = MutableStateFlow(LoginUiState())
    val loginState: StateFlow<LoginUiState> = _loginState.asStateFlow()

    private val _signUpState = MutableStateFlow(SignUpUiState())
    val signUpState: StateFlow<SignUpUiState> = _signUpState.asStateFlow()

    fun onLoginEmailChange(v: String) =
        _loginState.update { it.copy(email = v, emailError = null, authError = null) }

    fun onLoginPasswordChange(v: String) =
        _loginState.update { it.copy(password = v, passwordError = null, authError = null) }

    fun toggleLoginPasswordVisibility() =
        _loginState.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }

    fun fillDemoCredentials() {
        _loginState.update {
            it.copy(
                email = "student@cau.ac.kr",
                password = "password123",
                emailError = null,
                passwordError = null,
                authError = null
            )
        }
    }

    fun submitLogin(onSuccess: () -> Unit) {
        val s = _loginState.value
        if (s.isLoading) return
        val emailErr = Validators.validateLoginEmail(s.email)
        val pwErr = Validators.validatePassword(s.password)
        if (emailErr != null || pwErr != null) {
            _loginState.update { it.copy(emailError = emailErr, passwordError = pwErr) }
            return
        }

        viewModelScope.launch {
            _loginState.update { it.copy(isLoading = true, authError = null) }
            authRepository.login(s.email, s.password).fold(
                onSuccess = {
                    _loginState.update { it.copy(isLoading = false, infoMessage = null) }
                    onSuccess()
                },
                onFailure = { e ->
                    _loginState.update {
                        it.copy(
                            isLoading = false,
                            authError = e.message ?: "이메일 또는 비밀번호가 일치하지 않습니다."
                        )
                    }
                }
            )
        }
    }

    fun onSignUpEmailChange(v: String) =
        _signUpState.update { it.copy(email = v, emailError = null, submitError = null) }

    fun onSignUpNicknameChange(v: String) =
        _signUpState.update { it.copy(nickname = v, nicknameError = null, submitError = null) }

    fun onSignUpPasswordChange(v: String) =
        _signUpState.update { it.copy(password = v, passwordError = null, submitError = null) }

    fun onSignUpConfirmChange(v: String) =
        _signUpState.update { it.copy(confirmPassword = v, confirmError = null, submitError = null) }

    fun toggleSignUpPasswordVisibility() =
        _signUpState.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }

    fun toggleSignUpConfirmVisibility() =
        _signUpState.update { it.copy(isConfirmVisible = !it.isConfirmVisible) }

    fun submitSignUp(onSuccess: () -> Unit) {
        val s = _signUpState.value
        if (s.isLoading) return

        val emailErr = Validators.validateCauEmail(s.email)
        val nickErr = Validators.validateNickname(s.nickname)
        val pwErr = Validators.validatePassword(s.password)
        val confirmErr = if (s.password != s.confirmPassword) "비밀번호가 일치하지 않습니다" else null

        if (emailErr != null || nickErr != null || pwErr != null || confirmErr != null) {
            _signUpState.update {
                it.copy(
                    emailError = emailErr,
                    nicknameError = nickErr,
                    passwordError = pwErr,
                    confirmError = confirmErr
                )
            }
            return
        }

        viewModelScope.launch {
            _signUpState.update { it.copy(isLoading = true, submitError = null) }
            authRepository.signUp(s.email, s.password, s.nickname).fold(
                onSuccess = {
                    val registeredEmail = s.email.trim().lowercase()
                    _signUpState.value = SignUpUiState()
                    _loginState.update {
                        it.copy(
                            email = registeredEmail,
                            password = "",
                            infoMessage = "회원가입이 완료되었어요. 로그인해 주세요.",
                            authError = null
                        )
                    }
                    onSuccess()
                },
                onFailure = { e ->
                    _signUpState.update {
                        it.copy(
                            isLoading = false,
                            submitError = e.message ?: "회원가입에 실패했어요. 다시 시도해 주세요."
                        )
                    }
                }
            )
        }
    }

    companion object {
        fun factory(authRepository: AuthRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    AuthViewModel(authRepository) as T
            }
    }
}
