package com.luis.itera.domain.model

data class Exercise(
    val id: Long,
    val name: String,
    val category: String,
    val equipment: String,
    val mainMuscleGroup: String
)