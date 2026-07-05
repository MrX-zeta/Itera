package com.luis.itera.di

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.luis.itera.data.local.ExerciseSeed
import com.luis.itera.data.local.IteraDatabase
import com.luis.itera.data.local.dao.ExerciseDao
import com.luis.itera.data.local.dao.HydrationDao
import com.luis.itera.data.local.dao.SessionDao
import com.luis.itera.data.local.dao.SetDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Provider
import javax.inject.Singleton
import androidx.room.migration.Migration
import com.luis.itera.data.local.dao.RoutineDao
import com.luis.itera.data.local.dao.StatisticsDao

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context,
        exerciseDaoProvider: Provider<ExerciseDao>
    ): IteraDatabase =
        Room.databaseBuilder(context, IteraDatabase::class.java, "itera.db")
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7)
            .addCallback(object : RoomDatabase.Callback() {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
                        exerciseDaoProvider.get().insertAll(ExerciseSeed.exercises)
                    }
                }
            })
            .build()

    @Provides
    fun provideExerciseDao(db: IteraDatabase): ExerciseDao = db.exerciseDao()

    @Provides
    fun provideSessionDao(db: IteraDatabase): SessionDao = db.sessionDao()

    @Provides
    fun provideSetDao(db: IteraDatabase): SetDao = db.setDao()

    @Provides
    fun provideHydrationDao(db: IteraDatabase): HydrationDao = db.hydrationDao()

    @Provides
    fun provideStatisticsDao(db: IteraDatabase): StatisticsDao = db.statisticsDao()

    @Provides
    fun provideRoutineDao(db: IteraDatabase): RoutineDao = db.routineDao()
}

private val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE sessions ADD COLUMN focus TEXT")
    }
}

private val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE sessions ADD COLUMN startEpochMillis INTEGER NOT NULL DEFAULT 0")
    }
}

private val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE sets ADD COLUMN durationSeconds INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE sets ADD COLUMN intensity INTEGER NOT NULL DEFAULT 0")
    }
}

private val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE sets ADD COLUMN workSeconds INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE sets ADD COLUMN restSeconds INTEGER NOT NULL DEFAULT 0")
    }
}

private val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE sets ADD COLUMN isPr INTEGER NOT NULL DEFAULT 0")
    }
}

private val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS routines (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, name TEXT NOT NULL, focus TEXT)")
        db.execSQL("CREATE TABLE IF NOT EXISTS routine_exercises (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, routineId INTEGER NOT NULL, exerciseId INTEGER NOT NULL, displayOrder INTEGER NOT NULL, FOREIGN KEY(routineId) REFERENCES routines(id) ON DELETE CASCADE, FOREIGN KEY(exerciseId) REFERENCES exercises(id) ON DELETE CASCADE)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_routine_exercises_routineId ON routine_exercises(routineId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_routine_exercises_exerciseId ON routine_exercises(exerciseId)")
    }
}