package com.cau.campusmarket.ui.auth

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
import androidx.compose.material3.TextButton
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
import com.cau.campusmarket.di.AppConfig
import com.cau.campusmarket.ui.components.FigmaErrorBanner
import com.cau.campusmarket.ui.components.FigmaPrimaryButton
import com.cau.campusmarket.ui.theme.BorderGray
import com.cau.campusmarket.ui.theme.DangerRed
import com.cau.campusmarket.ui.theme.PrimaryBlue
import com.cau.campusmarket.ui.theme.PrimaryBlueSoftBg
import com.cau.campusmarket.ui.theme.TextHint
import com.cau.campusmarket.ui.theme.TextSecondary

/**
 * Figma 1-1 / 1-2 로그인 화면
 */
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
            // Figma 파란 둥근 사각형 LOGO 배지
            Surface(
                color = PrimaryBlue,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.size(64.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "CAU",
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(Modifier.height(14.dp))
            Text(
                text = "중앙대 캠퍼스 마켓",
                style = MaterialTheme.typography.headlineSmall
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "우리 학교 학생끼리 하는 중고거래",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )

            Spacer(Modifier.height(32.dp))

            state.infoMessage?.let { info ->
                Surface(
                    color = PrimaryBlueSoftBg,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 14.dp)
                ) {
                    Text(
                        text = info,
                        color = PrimaryBlue,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            // 학교 이메일 라벨 + 입력창
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
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = BorderGray,
                        focusedBorderColor = PrimaryBlue
                    ),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier.fillMaxWidth()
                )
                state.emailError?.let {
                    Spacer(Modifier.height(4.dp))
                    Text(it, color = DangerRed, style = MaterialTheme.typography.labelMedium)
                }
            }

            Spacer(Modifier.height(14.dp))

            // 비밀번호 라벨 + 입력창
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
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = BorderGray,
                        focusedBorderColor = PrimaryBlue
                    ),
                    visualTransformation = if (state.isPasswordVisible) {
                        VisualTransformation.None
                    } else {
                        PasswordVisualTransformation()
                    },
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

            // Figma 1-2: 계정 정보 불일치·네트워크 오류는 버튼 위 연분홍 안내 영역에 표시
            state.authError?.let { err ->
                Spacer(Modifier.height(14.dp))
                FigmaErrorBanner(message = err)
            }

            Spacer(Modifier.height(20.dp))

            FigmaPrimaryButton(
                text = if (state.isLoading) "로그인 중…" else "로그인",
                onClick = { viewModel.submitLogin(onLoginSuccess) },
                enabled = !state.isLoading,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(16.dp))

            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("계정이 없으신가요? ", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                Text(
                    text = "회원가입",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = PrimaryBlue,
                    modifier = Modifier.clickable(enabled = !state.isLoading, onClick = onNavigateToSignUp)
                )
            }

            if (!AppConfig.USE_FIREBASE) {
                Spacer(Modifier.height(18.dp))
                TextButton(onClick = viewModel::fillDemoCredentials) {
                    Text("데모 계정 자동 입력 (student@cau.ac.kr)", color = TextSecondary)
                }
            }
        }
    }
}
