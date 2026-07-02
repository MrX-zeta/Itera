package com.luis.itera.domain.model

data class HydrationIntake(
    val id: Long,
    val dateTimeEpochMillis: Long,
    val amountMl: Int
)