package com.caumarket.app.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.caumarket.app.core.error.MarketErrorCode
import com.caumarket.app.core.error.MarketException
import com.caumarket.app.core.model.User
import com.caumarket.app.core.validation.Validation
import com.caumarket.app.data.repository.AuthRepository
import com.caumarket.app.ui.common.MarketErrorMapper
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
    val isLoading: Boolean = false,
    val needsProfileCompletion: Boolean = false,
    val completionNickname: String = ""
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

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    private val _isCheckingSession = MutableStateFlow(true)
    val isCheckingSession: StateFlow<Boolean> = _isCheckingSession.asStateFlow()

    private val _loginState = MutableStateFlow(LoginUiState())
    val loginState: StateFlow<LoginUiState> = _loginState.asStateFlow()

    private val _signUpState = MutableStateFlow(SignUpUiState())
    val signUpState: StateFlow<SignUpUiState> = _signUpState.asStateFlow()

    init {
        refreshSession()
    }

    fun refreshSession() {
        viewModelScope.launch {
            _isCheckingSession.value = true
            authRepository.getCurrentUser().onSuccess { user ->
                _currentUser.value = user
            }
            _isCheckingSession.value = false
        }
    }

    fun onLoginEmailChange(v: String) =
        _loginState.update { it.copy(email = v, emailError = null, authError = null) }

    fun onLoginPasswordChange(v: String) =
        _loginState.update { it.copy(password = v, passwordError = null, authError = null) }

    fun toggleLoginPasswordVisibility() =
        _loginState.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }

    fun onCompletionNicknameChange(v: String) =
        _loginState.update { it.copy(completionNickname = v, authError = null) }

    fun submitLogin(onSuccess: () -> Unit) {
        val s = _loginState.value
        if (s.isLoading) return

        val emailErr = runCatching { Validation.email(s.email) }.exceptionOrNull()?.let { MarketErrorMapper.toMessage(it) }
        val pwErr = if (s.password.isEmpty()) "비밀번호를 입력해 주세요." else null
        if (emailErr != null || pwErr != null) {
            _loginState.update { it.copy(emailError = emailErr, passwordError = pwErr) }
            return
        }

        viewModelScope.launch {
            _loginState.update { it.copy(isLoading = true, authError = null) }
            authRepository.login(s.email, s.password).fold(
                onSuccess = { user ->
                    _currentUser.value = user
                    _loginState.update {
                        it.copy(isLoading = false, infoMessage = null, needsProfileCompletion = false)
                    }
                    onSuccess()
                },
                onFailure = { e ->
                    val incomplete = (e as? MarketException)?.code == MarketErrorCode.INCOMPLETE_PROFILE
                    _loginState.update {
                        it.copy(
                            isLoading = false,
                            needsProfileCompletion = incomplete,
                            authError = MarketErrorMapper.toMessage(e)
                        )
                    }
                }
            )
        }
    }

    fun submitCompleteSignUp(onSuccess: () -> Unit) {
        val s = _loginState.value
        if (s.isLoading) return
        viewModelScope.launch {
            _loginState.update { it.copy(isLoading = true, authError = null) }
            authRepository.completeSignUp(s.completionNickname).fold(
                onSuccess = {
                    authRepository.getCurrentUser().onSuccess { user ->
                        _currentUser.value = user
                        _loginState.update {
                            it.copy(isLoading = false, needsProfileCompletion = false, completionNickname = "")
                        }
                        if (user != null) onSuccess()
                    }
                },
                onFailure = { e ->
                    _loginState.update {
                        it.copy(isLoading = false, authError = MarketErrorMapper.toMessage(e))
                    }
                }
            )
        }
    }

    fun logout(onLoggedOut: () -> Unit) {
        viewModelScope.launch {
            authRepository.logout()
            _currentUser.value = null
            onLoggedOut()
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

        val emailErr = runCatching { Validation.email(s.email) }.exceptionOrNull()?.let { MarketErrorMapper.toMessage(it) }
        val nickErr = runCatching { Validation.nickname(s.nickname) }.exceptionOrNull()?.let { MarketErrorMapper.toMessage(it) }
        val pwErr = runCatching { Validation.password(s.password) }.exceptionOrNull()?.let { MarketErrorMapper.toMessage(it) }
        val confirmErr = Validation.validatePasswordConfirmation(s.password, s.confirmPassword)
            .exceptionOrNull()?.let { MarketErrorMapper.toMessage(it) }

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
                    val registered = s.email.trim().lowercase()
                    _signUpState.value = SignUpUiState()
                    _loginState.update {
                        it.copy(
                            email = registered,
                            password = "",
                            infoMessage = "회원가입이 완료되었어요. 로그인해 주세요.",
                            authError = null
                        )
                    }
                    onSuccess()
                },
                onFailure = { e ->
                    _signUpState.update {
                        it.copy(isLoading = false, submitError = MarketErrorMapper.toMessage(e))
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
