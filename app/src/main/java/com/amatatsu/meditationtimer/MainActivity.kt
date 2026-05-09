package com.amatatsu.meditationtimer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.amatatsu.meditationtimer.ui.theme.MeditationTimerTheme
import java.text.SimpleDateFormat
import java.util.*

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
                AppRoot()
            }
        }
    }
}

@Composable
fun AppRoot(authVm: AuthViewModel = viewModel()) {
    val authState by authVm.authState.collectAsState()

    when (authState) {
        is AuthState.Loading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Teal)
            }
        }
        is AuthState.Authenticated -> {
            TimerScreen(onSignOut = { authVm.signOut() })
        }
        else -> {
            AuthScreen(authVm = authVm)
        }
    }
}

@Composable
fun TimerScreen(vm: TimerViewModel = viewModel(), onSignOut: () -> Unit = {}) {
    val context = LocalContext.current
    val selectedMinutes by vm.selectedMinutes.collectAsState()
    val remainingSeconds by vm.remainingSeconds.collectAsState()
    val timerState by vm.timerState.collectAsState()
    val sessions by vm.sessions.collectAsState()

    LaunchedEffect(Unit) {
        vm.initDb(context)
        vm.initSound(context)
    }

    LaunchedEffect(timerState) {
        if (timerState == TimerState.FINISHED) vm.playBellAndVibrate(context)
    }

    val displaySeconds = if (timerState == TimerState.IDLE) selectedMinutes * 60 else remainingSeconds
    val displayColor = when (timerState) {
        TimerState.RUNNING, TimerState.FINISHED -> Teal
        else -> TextPrimary
    }

    Scaffold(containerColor = Color.White) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                Spacer(modifier = Modifier.height(48.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("瞑想タイマー", fontSize = 14.sp, color = TextMuted, letterSpacing = 2.sp)
                    TextButton(onClick = onSignOut) {
                        Text("ログアウト", fontSize = 12.sp, color = TextMuted)
                    }
                }

                Spacer(modifier = Modifier.height(40.dp))

                // 残り時間
                Text(
                    text = formatTime(displaySeconds),
                    fontSize = 80.sp,
                    fontWeight = FontWeight.Light,
                    color = displayColor,
                    textAlign = TextAlign.Center,
                    letterSpacing = 2.sp
                )

                Spacer(modifier = Modifier.height(24.dp))

                // 分数 +/-
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    TextButton(
                        onClick = { vm.setMinutes(selectedMinutes - 1) },
                        enabled = timerState == TimerState.IDLE && selectedMinutes > 1
                    ) { Text("－", fontSize = 22.sp, color = TextMuted) }

                    Text("${selectedMinutes}分", fontSize = 18.sp, color = TextMuted)

                    TextButton(
                        onClick = { vm.setMinutes(selectedMinutes + 1) },
                        enabled = timerState == TimerState.IDLE && selectedMinutes < 120
                    ) { Text("＋", fontSize = 22.sp, color = TextMuted) }
                }

                Spacer(modifier = Modifier.height(40.dp))

                // 開始ボタン
                Button(
                    onClick = {
                        when (timerState) {
                            TimerState.IDLE, TimerState.PAUSED -> vm.start()
                            TimerState.FINISHED -> vm.reset()
                            else -> {}
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
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

                Spacer(modifier = Modifier.height(12.dp))

                // 一時停止・リセット
                Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
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

                // 履歴セクション
                if (sessions.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(32.dp))
                    HorizontalDivider(color = Surface)
                    Spacer(modifier = Modifier.height(20.dp))

                    val totalMinutes = sessions.sumOf { it.durationMinutes }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("履歴", fontSize = 13.sp, color = TextMuted, letterSpacing = 1.sp)
                        Text("累計 ${totalMinutes} 分", fontSize = 13.sp, color = Teal)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    CalendarSection(sessions = sessions)

                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
}

// 日付キー "yyyy-MM-dd" → その日の合計分数 のMapを作る
fun buildDayMap(sessions: List<MeditationSession>): Map<String, Int> {
    val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.JAPAN)
    return sessions.groupBy { fmt.format(Date(it.startedAt)) }
        .mapValues { (_, list) -> list.sumOf { it.durationMinutes } }
}

@Composable
fun CalendarSection(sessions: List<MeditationSession>) {
    val today = remember { Calendar.getInstance() }
    var calYear by remember { mutableIntStateOf(today.get(Calendar.YEAR)) }
    var calMonth by remember { mutableIntStateOf(today.get(Calendar.MONTH)) } // 0-indexed
    var selectedDay by remember { mutableStateOf<String?>(null) }

    val dayMap = remember(sessions) { buildDayMap(sessions) }

    // 月ナビゲーション
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextButton(onClick = {
            if (calMonth == 0) { calMonth = 11; calYear-- } else calMonth--
            selectedDay = null
        }) { Text("‹", fontSize = 24.sp, color = TextMuted) }

        Text(
            text = "${calYear}年 ${calMonth + 1}月",
            fontSize = 15.sp,
            color = TextPrimary,
            fontWeight = FontWeight.Medium
        )

        TextButton(onClick = {
            if (calMonth == 11) { calMonth = 0; calYear++ } else calMonth++
            selectedDay = null
        }) { Text("›", fontSize = 24.sp, color = TextMuted) }
    }

    Spacer(modifier = Modifier.height(8.dp))

    // 曜日ヘッダー
    val dows = listOf("日", "月", "火", "水", "木", "金", "土")
    Row(modifier = Modifier.fillMaxWidth()) {
        dows.forEach { d ->
            Text(
                text = d,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                fontSize = 11.sp,
                color = TextMuted
            )
        }
    }

    Spacer(modifier = Modifier.height(4.dp))

    // カレンダーグリッドを構築
    val firstDow = Calendar.getInstance().apply {
        set(calYear, calMonth, 1)
    }.get(Calendar.DAY_OF_WEEK) - 1 // 0=日曜

    val daysInMonth = Calendar.getInstance().apply {
        set(calYear, calMonth + 1, 0)
    }.get(Calendar.DAY_OF_MONTH)

    val cells = firstDow + daysInMonth
    val rows = (cells + 6) / 7

    val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.JAPAN)
    val todayKey = fmt.format(today.time)

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        for (row in 0 until rows) {
            Row(modifier = Modifier.fillMaxWidth()) {
                for (col in 0..6) {
                    val cellIndex = row * 7 + col
                    val day = cellIndex - firstDow + 1
                    val inMonth = day in 1..daysInMonth
                    val key = if (inMonth) "%04d-%02d-%02d".format(calYear, calMonth + 1, day) else null
                    val mins = key?.let { dayMap[it] }
                    val isToday = key == todayKey
                    val isSelected = key != null && key == selectedDay

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                when {
                                    isSelected -> Teal
                                    mins != null -> TealDim
                                    else -> Color.Transparent
                                }
                            )
                            .then(
                                if (key != null && mins != null)
                                    Modifier.clickable {
                                        selectedDay = if (selectedDay == key) null else key
                                    }
                                else Modifier
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (inMonth) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "$day",
                                    fontSize = 13.sp,
                                    fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                                    color = when {
                                        isSelected -> Color.White
                                        isToday -> Teal
                                        inMonth -> TextPrimary
                                        else -> Color.Transparent
                                    }
                                )
                                if (mins != null) {
                                    Text(
                                        text = "${mins}m",
                                        fontSize = 9.sp,
                                        color = if (isSelected) Color.White.copy(alpha = 0.85f) else Teal
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // 選択日の詳細
    selectedDay?.let { key ->
        val daySessions = sessions.filter {
            SimpleDateFormat("yyyy-MM-dd", Locale.JAPAN).format(Date(it.startedAt)) == key
        }
        if (daySessions.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            val (y, m, d) = key.split("-")
            val total = daySessions.sumOf { it.durationMinutes }
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Surface,
                shape = RoundedCornerShape(10.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        "${m.toInt()}月${d.toInt()}日 — 合計 ${total} 分",
                        fontSize = 13.sp,
                        color = TextPrimary,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    daySessions.forEach { s ->
                        val time = SimpleDateFormat("HH:mm", Locale.JAPAN).format(Date(s.startedAt))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(time, fontSize = 13.sp, color = TextMuted)
                            Text("${s.durationMinutes} 分", fontSize = 13.sp, color = TextPrimary)
                        }
                    }
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
