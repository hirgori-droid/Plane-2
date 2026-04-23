package com.example.russiantrainer.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [WordEntity::class, SessionResultEntity::class],
    version = 7,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun wordDao(): WordDao
}
