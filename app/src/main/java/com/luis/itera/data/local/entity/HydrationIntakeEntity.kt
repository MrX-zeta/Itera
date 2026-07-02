package com.luis.itera.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "hydration_intakes",
    indices = [Index("dateTimeEpochMillis")]
)
data class HydrationIntakeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dateTimeEpochMillis: Long,
    val amountMl: Int
)