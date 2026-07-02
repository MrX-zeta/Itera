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
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
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