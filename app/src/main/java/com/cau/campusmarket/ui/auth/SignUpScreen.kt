package com.cau.campusmarket.ui.auth

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cau.campusmarket.ui.components.FigmaErrorBanner
import com.cau.campusmarket.ui.components.FigmaPrimaryButton
import com.cau.campusmarket.ui.theme.BorderGray
import com.cau.campusmarket.ui.theme.DangerRed
import com.cau.campusmarket.ui.theme.PrimaryBlue
import com.cau.campusmarket.ui.theme.TextHint
import com.cau.campusmarket.ui.theme.TextSecondary

/**
 * Figma 1-3 회원가입 화면
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignUpScreen(
    viewModel: AuthViewModel,
    onBack: () -> Unit,
    onSignUpSuccess: () -> Unit
) {
    val state by viewModel.signUpState.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = Color.White,
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("회원가입", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "뒤로 가기")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
                )
                HorizontalDivider(color = BorderGray)
            }
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp)
            ) {
                FigmaPrimaryButton(
                    text = if (state.isLoading) "가입 중…" else "가입하기",
                    onClick = { viewModel.submitSignUp(onSignUpSuccess) },
                    enabled = !state.isLoading,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp, vertical = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text("학교 이메일", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(6.dp))
            OutlinedTextField(
                value = state.email,
                onValueChange = viewModel::onSignUpEmailChange,
                placeholder = { Text("abc@cau.ac.kr", color = TextHint) },
                singleLine = true,
                enabled = !state.isLoading,
                isError = state.emailError != null,
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = BorderGray, focusedBorderColor = PrimaryBlue),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = state.emailError ?: "cau.ac.kr 학교 이메일만 가입할 수 있어요",
                color = if (state.emailError != null) DangerRed else TextSecondary,
                style = MaterialTheme.typography.labelMedium
            )

            Spacer(Modifier.height(16.dp))

            Text("닉네임", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(6.dp))
            OutlinedTextField(
                value = state.nickname,
                onValueChange = viewModel::onSignUpNicknameChange,
                placeholder = { Text("앱에서 사용할 이름 (2~10자)", color = TextHint) },
                singleLine = true,
                enabled = !state.isLoading,
                isError = state.nicknameError != null,
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = BorderGray, focusedBorderColor = PrimaryBlue),
                modifier = Modifier.fillMaxWidth()
            )
            state.nicknameError?.let {
                Spacer(Modifier.height(4.dp))
                Text(it, color = DangerRed, style = MaterialTheme.typography.labelMedium)
            }

            Spacer(Modifier.height(16.dp))

            Text("비밀번호", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(6.dp))
            OutlinedTextField(
                value = state.password,
                onValueChange = viewModel::onSignUpPasswordChange,
                placeholder = { Text("비밀번호 입력", color = TextHint) },
                singleLine = true,
                enabled = !state.isLoading,
                isError = state.passwordError != null,
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = BorderGray, focusedBorderColor = PrimaryBlue),
                visualTransformation = if (state.isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = viewModel::toggleSignUpPasswordVisibility) {
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

            Spacer(Modifier.height(16.dp))

            Text("비밀번호 확인", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(6.dp))
            OutlinedTextField(
                value = state.confirmPassword,
                onValueChange = viewModel::onSignUpConfirmChange,
                placeholder = { Text("비밀번호 다시 입력", color = TextHint) },
                singleLine = true,
                enabled = !state.isLoading,
                isError = state.confirmError != null,
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = BorderGray, focusedBorderColor = PrimaryBlue),
                visualTransformation = if (state.isConfirmVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = viewModel::toggleSignUpConfirmVisibility) {
                        Icon(
                            imageVector = if (state.isConfirmVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                            contentDescription = "비밀번호 확인 보기",
                            tint = TextSecondary
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
            state.confirmError?.let {
                Spacer(Modifier.height(4.dp))
                Text(it, color = DangerRed, style = MaterialTheme.typography.labelMedium)
            }

            state.submitError?.let { err ->
                Spacer(Modifier.height(16.dp))
                FigmaErrorBanner(message = err)
            }
        }
    }
}
