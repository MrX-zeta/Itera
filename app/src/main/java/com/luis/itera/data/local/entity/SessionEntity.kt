package com.luis.itera.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "sessions",
    indices = [Index("dateEpochDay")]
)
data class SessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dateEpochDay: Long,
    val durationMinutes: Int = 0,
    val notes: String? = null,
    val isFinished: Boolean = false
)