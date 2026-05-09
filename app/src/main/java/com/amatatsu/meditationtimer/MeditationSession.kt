package com.amatatsu.meditationtimer

import androidx.room.Entity
import androidx.room.PrimaryKey

// 瞑想セッションの1レコードを表すテーブル定義
@Entity(tableName = "meditation_sessions")
data class MeditationSession(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startedAt: Long,       // エポックミリ秒
    val durationMinutes: Int   // 瞑想した分数
)
