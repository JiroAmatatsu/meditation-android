package com.amatatsu.meditationtimer

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun AuthScreen(authVm: AuthViewModel = viewModel()) {
    val context = LocalContext.current
    val authState by authVm.authState.collectAsState()

    var isSignUp by remember { mutableStateOf(false) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    val isLoading = authState is AuthState.Loading

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "瞑想タイマー",
            fontSize = 14.sp,
            color = TextMuted,
            letterSpacing = 2.sp
        )

        Spacer(modifier = Modifier.height(48.dp))

        // Sign In / Sign Up トグル
        Row {
            TextButton(onClick = { isSignUp = false }) {
                Text(
                    "ログイン",
                    color = if (!isSignUp) Teal else TextMuted,
                    fontWeight = if (!isSignUp) FontWeight.Bold else FontWeight.Normal,
                    fontSize = 15.sp
                )
            }
            Text("  /  ", color = TextMuted, modifier = Modifier.align(Alignment.CenterVertically))
            TextButton(onClick = { isSignUp = true }) {
                Text(
                    "新規登録",
                    color = if (isSignUp) Teal else TextMuted,
                    fontWeight = if (isSignUp) FontWeight.Bold else FontWeight.Normal,
                    fontSize = 15.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // メール
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("メールアドレス", color = TextMuted) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Teal,
                unfocusedBorderColor = Color(0xFFDDE3EC),
                focusedLabelColor = Teal,
                cursorColor = Teal
            ),
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        // パスワード
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("パスワード", color = TextMuted) },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Teal,
                unfocusedBorderColor = Color(0xFFDDE3EC),
                focusedLabelColor = Teal,
                cursorColor = Teal
            ),
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // メイン操作ボタン
        Button(
            onClick = {
                if (isSignUp) authVm.signUp(email, password)
                else authVm.signIn(email, password)
            },
            enabled = !isLoading && email.isNotBlank() && password.isNotBlank(),
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Teal)
        ) {
            if (isLoading) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
            } else {
                Text(
                    text = if (isSignUp) "登録" else "ログイン",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 区切り線
        Row(verticalAlignment = Alignment.CenterVertically) {
            HorizontalDivider(modifier = Modifier.weight(1f), color = Color(0xFFDDE3EC))
            Text("  または  ", fontSize = 12.sp, color = TextMuted)
            HorizontalDivider(modifier = Modifier.weight(1f), color = Color(0xFFDDE3EC))
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Google ログインボタン
        OutlinedButton(
            onClick = { authVm.signInWithGoogle(context) },
            enabled = !isLoading,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(14.dp),
            border = ButtonDefaults.outlinedButtonBorder.copy(
                width = 1.dp
            ),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary)
        ) {
            Text("Google でログイン", fontSize = 15.sp)
        }

        // エラー表示
        if (authState is AuthState.Error) {
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = (authState as AuthState.Error).message,
                color = Color(0xFFE05252),
                fontSize = 13.sp
            )
        }
    }
}
