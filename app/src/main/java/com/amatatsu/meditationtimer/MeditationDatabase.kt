package com.amatatsu.meditationtimer

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [MeditationSession::class], version = 1)
abstract class MeditationDatabase : RoomDatabase() {
    abstract fun meditationDao(): MeditationDao

    companion object {
        @Volatile private var INSTANCE: MeditationDatabase? = null

        fun getInstance(context: Context): MeditationDatabase =
            INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    MeditationDatabase::class.java,
                    "meditation.db"
                ).build().also { INSTANCE = it }
            }
    }
}
