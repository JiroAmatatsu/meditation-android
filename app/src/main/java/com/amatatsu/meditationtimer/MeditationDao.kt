package com.amatatsu.meditationtimer

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MeditationDao {
    // 新しい順に全レコードを取得(Flow で変更を自動通知)
    @Query("SELECT * FROM meditation_sessions ORDER BY startedAt DESC")
    fun getAll(): Flow<List<MeditationSession>>

    @Insert
    suspend fun insert(session: MeditationSession)
}
