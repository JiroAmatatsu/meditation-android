package com.amatatsu.meditationtimer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.amatatsu.meditationtimer.ui.theme.MeditationTimerTheme

// アクセントカラー(HTML版と統一)
val Teal = Color(0xFF4AACAC)
val TealDim = Color(0xFFCDE9E9)
val TextPrimary = Color(0xFF3A3F4A)
val TextMuted = Color(0xFF8A95A8)
val Surface = Color(0xFFF3F6F8)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MeditationTimerTheme {
                TimerScreen()
            }
        }
    }
}

@Composable
fun TimerScreen(vm: TimerViewModel = viewModel()) {
    val selectedMinutes by vm.selectedMinutes.collectAsState()
    val remainingSeconds by vm.remainingSeconds.collectAsState()
    val timerState by vm.timerState.collectAsState()

    val displaySeconds = if (timerState == TimerState.IDLE) selectedMinutes * 60 else remainingSeconds
    val displayColor = when (timerState) {
        TimerState.RUNNING, TimerState.FINISHED -> Teal
        else -> TextPrimary
    }

    Scaffold(
        containerColor = Color.White
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

            // タイトル
            Text(
                text = "瞑想タイマー",
                fontSize = 14.sp,
                color = TextMuted,
                letterSpacing = 2.sp
            )

            Spacer(modifier = Modifier.height(48.dp))

            // 残り時間の大きい表示
            Text(
                text = formatTime(displaySeconds),
                fontSize = 80.sp,
                fontWeight = FontWeight.Light,
                color = displayColor,
                textAlign = TextAlign.Center,
                letterSpacing = 2.sp
            )

            Spacer(modifier = Modifier.height(32.dp))

            // 分数の +/- 調整(IDLE時のみ操作可能)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                TextButton(
                    onClick = { vm.setMinutes(selectedMinutes - 1) },
                    enabled = timerState == TimerState.IDLE && selectedMinutes > 1
                ) {
                    Text("－", fontSize = 22.sp, color = TextMuted)
                }
                Text(
                    text = "${selectedMinutes}分",
                    fontSize = 18.sp,
                    color = TextMuted
                )
                TextButton(
                    onClick = { vm.setMinutes(selectedMinutes + 1) },
                    enabled = timerState == TimerState.IDLE && selectedMinutes < 120
                ) {
                    Text("＋", fontSize = 22.sp, color = TextMuted)
                }
            }

            Spacer(modifier = Modifier.height(48.dp))

            // 開始ボタン(全幅)
            Button(
                onClick = {
                    when (timerState) {
                        TimerState.IDLE, TimerState.PAUSED -> vm.start()
                        TimerState.FINISHED -> vm.reset()
                        else -> {}
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Teal)
            ) {
                Text(
                    text = when (timerState) {
                        TimerState.IDLE -> "開始"
                        TimerState.PAUSED -> "再開"
                        TimerState.FINISHED -> "もう一度"
                        TimerState.RUNNING -> "開始"
                    },
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 一時停止・リセット(テキストボタン)
            Row(
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                TextButton(
                    onClick = { vm.pause() },
                    enabled = timerState == TimerState.RUNNING
                ) {
                    Text("一時停止", color = if (timerState == TimerState.RUNNING) TextMuted else Color.Transparent)
                }
                TextButton(
                    onClick = { vm.reset() },
                    enabled = timerState != TimerState.IDLE
                ) {
                    Text("リセット", color = if (timerState != TimerState.IDLE) TextMuted else Color.Transparent)
                }
            }
        }
    }
}

fun formatTime(totalSeconds: Int): String {
    val m = totalSeconds / 60
    val s = totalSeconds % 60
    return "%02d:%02d".format(m, s)
}
