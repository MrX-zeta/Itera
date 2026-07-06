package com.luis.itera.domain.model

data class Routine(
    val id: Long,
    val name: String,
    val focus: String?,
    val exerciseIds: List<Long>
)