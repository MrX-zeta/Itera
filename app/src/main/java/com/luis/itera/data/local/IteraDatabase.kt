package com.luis.itera.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.luis.itera.data.local.dao.ExerciseDao
import com.luis.itera.data.local.dao.HydrationDao
import com.luis.itera.data.local.dao.RoutineDao
import com.luis.itera.data.local.dao.SessionDao
import com.luis.itera.data.local.dao.SetDao
import com.luis.itera.data.local.dao.StatisticsDao
import com.luis.itera.data.local.entity.DailyHydrationGoalEntity
import com.luis.itera.data.local.entity.ExerciseEntity
import com.luis.itera.data.local.entity.HydrationIntakeEntity
import com.luis.itera.data.local.entity.RoutineEntity
import com.luis.itera.data.local.entity.RoutineExerciseEntity
import com.luis.itera.data.local.entity.SessionEntity
import com.luis.itera.data.local.entity.SetEntity

@Database(
    entities = [
        ExerciseEntity::class,
        SessionEntity::class,
        SetEntity::class,
        HydrationIntakeEntity::class,
        DailyHydrationGoalEntity::class,
        RoutineEntity::class,
        RoutineExerciseEntity::class
    ],
    version = 8,
    exportSchema = true
)
abstract class IteraDatabase : RoomDatabase() {
    abstract fun exerciseDao(): ExerciseDao
    abstract fun sessionDao(): SessionDao
    abstract fun setDao(): SetDao
    abstract fun hydrationDao(): HydrationDao

    abstract fun statisticsDao(): StatisticsDao
    abstract fun routineDao(): RoutineDao
}