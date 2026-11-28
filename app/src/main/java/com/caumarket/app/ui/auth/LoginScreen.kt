package com.caumarket.app.ui.auth

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.caumarket.app.ui.components.FigmaErrorBanner
import com.caumarket.app.ui.components.FigmaPrimaryButton
import com.caumarket.app.ui.theme.BorderGray
import com.caumarket.app.ui.theme.DangerRed
import com.caumarket.app.ui.theme.PrimaryBlue
import com.caumarket.app.ui.theme.PrimaryBlueSoftBg
import com.caumarket.app.ui.theme.TextHint
import com.caumarket.app.ui.theme.TextSecondary

@Composable
fun LoginScreen(
    viewModel: AuthViewModel,
    onLoginSuccess: () -> Unit,
    onNavigateToSignUp: () -> Unit
) {
    val state by viewModel.loginState.collectAsStateWithLifecycle()

    Scaffold(containerColor = Color.White) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(color = PrimaryBlue, shape = RoundedCornerShape(16.dp), modifier = Modifier.size(64.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Text("CAU", color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(Modifier.height(14.dp))
            Text("중앙대 캠퍼스 마켓", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(4.dp))
            Text("우리 학교 학생끼리 하는 중고거래", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
            Spacer(Modifier.height(32.dp))

            state.infoMessage?.let { info ->
                Surface(
                    color = PrimaryBlueSoftBg,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp)
                ) {
                    Text(info, color = PrimaryBlue, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(12.dp))
                }
            }

            Column(modifier = Modifier.fillMaxWidth()) {
                Text("학교 이메일", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = state.email,
                    onValueChange = viewModel::onLoginEmailChange,
                    placeholder = { Text("example@cau.ac.kr", color = TextHint) },
                    singleLine = true,
                    enabled = !state.isLoading,
                    isError = state.emailError != null,
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = BorderGray, focusedBorderColor = PrimaryBlue),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier.fillMaxWidth()
                )
                state.emailError?.let {
                    Spacer(Modifier.height(4.dp))
                    Text(it, color = DangerRed, style = MaterialTheme.typography.labelMedium)
                }
            }

            Spacer(Modifier.height(14.dp))

            Column(modifier = Modifier.fillMaxWidth()) {
                Text("비밀번호", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = state.password,
                    onValueChange = viewModel::onLoginPasswordChange,
                    placeholder = { Text("비밀번호 입력", color = TextHint) },
                    singleLine = true,
                    enabled = !state.isLoading,
                    isError = state.passwordError != null,
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = BorderGray, focusedBorderColor = PrimaryBlue),
                    visualTransformation = if (state.isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = viewModel::toggleLoginPasswordVisibility) {
                            Icon(
                                imageVector = if (state.isPasswordVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                contentDescription = "비밀번호 보기",
                                tint = TextSecondary
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                state.passwordError?.let {
                    Spacer(Modifier.height(4.dp))
                    Text(it, color = DangerRed, style = MaterialTheme.typography.labelMedium)
                }
            }

            if (state.needsProfileCompletion) {
                Spacer(Modifier.height(14.dp))
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text("닉네임 마무리 등록 (2~10자)", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(6.dp))
                    OutlinedTextField(
                        value = state.completionNickname,
                        onValueChange = viewModel::onCompletionNicknameChange,
                        placeholder = { Text("앱에서 사용할 닉네임", color = TextHint) },
                        singleLine = true,
                        enabled = !state.isLoading,
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = BorderGray, focusedBorderColor = PrimaryBlue),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            state.authError?.let { err ->
                Spacer(Modifier.height(14.dp))
                FigmaErrorBanner(message = err)
            }

            Spacer(Modifier.height(20.dp))

            FigmaPrimaryButton(
                text = when {
                    state.isLoading -> "로그인 중…"
                    state.needsProfileCompletion -> "프로필 완료 후 로그인"
                    else -> "로그인"
                },
                onClick = {
                    if (state.needsProfileCompletion) viewModel.submitCompleteSignUp(onLoginSuccess)
                    else viewModel.submitLogin(onLoginSuccess)
                },
                enabled = !state.isLoading,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(16.dp))

            Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                Text("계정이 없으신가요? ", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                Text(
                    text = "회원가입",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = PrimaryBlue,
                    modifier = Modifier.clickable(enabled = !state.isLoading, onClick = onNavigateToSignUp)
                )
            }
        }
    }
}
