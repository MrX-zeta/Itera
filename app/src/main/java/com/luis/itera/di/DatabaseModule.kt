package com.luis.itera.di

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.paquete.itera.data.local.ExerciseSeed
import com.paquete.itera.data.local.IteraDatabase
import com.paquete.itera.data.local.dao.ExerciseDao
import com.paquete.itera.data.local.dao.HydrationDao
import com.paquete.itera.data.local.dao.SessionDao
import com.paquete.itera.data.local.dao.SetDao
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