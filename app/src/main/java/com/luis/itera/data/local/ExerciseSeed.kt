package com.luis.itera.data.local

import com.paquete.itera.data.local.entity.ExerciseEntity

object ExerciseSeed {
    val exercises = listOf(
        ExerciseEntity(name = "Dominadas pronas", category = "Calistenia", equipment = "Barra", mainMuscleGroup = "Espalda"),
        ExerciseEntity(name = "Dominadas supinas", category = "Calistenia", equipment = "Barra", mainMuscleGroup = "Espalda"),
        ExerciseEntity(name = "Fondos en paralelas", category = "Calistenia", equipment = "Peso Corporal", mainMuscleGroup = "Pecho"),
        ExerciseEntity(name = "Flexiones", category = "Calistenia", equipment = "Peso Corporal", mainMuscleGroup = "Pecho"),
        ExerciseEntity(name = "Muscle up", category = "Calistenia", equipment = "Barra", mainMuscleGroup = "Espalda"),
        ExerciseEntity(name = "Sentadilla pistol", category = "Calistenia", equipment = "Peso Corporal", mainMuscleGroup = "Piernas"),
        ExerciseEntity(name = "Press banca", category = "Gimnasio", equipment = "Barra", mainMuscleGroup = "Pecho"),
        ExerciseEntity(name = "Sentadilla con barra", category = "Gimnasio", equipment = "Barra", mainMuscleGroup = "Piernas"),
        ExerciseEntity(name = "Peso muerto", category = "Gimnasio", equipment = "Barra", mainMuscleGroup = "Espalda"),
        ExerciseEntity(name = "Press militar", category = "Gimnasio", equipment = "Barra", mainMuscleGroup = "Hombros"),
        ExerciseEntity(name = "Curl con mancuerna", category = "Gimnasio", equipment = "Mancuerna", mainMuscleGroup = "Bíceps"),
        ExerciseEntity(name = "Remo con mancuerna", category = "Gimnasio", equipment = "Mancuerna", mainMuscleGroup = "Espalda"),
        ExerciseEntity(name = "Carrera continua", category = "Cardio", equipment = "Peso Corporal", mainMuscleGroup = "Piernas"),
        ExerciseEntity(name = "Cuerda (saltos)", category = "Cardio", equipment = "Peso Corporal", mainMuscleGroup = "Piernas")
    )
}