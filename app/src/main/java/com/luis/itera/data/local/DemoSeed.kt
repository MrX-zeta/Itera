package com.luis.itera.data.local

import com.luis.itera.data.local.dao.ExerciseDao
import com.luis.itera.data.local.dao.HydrationDao
import com.luis.itera.data.local.dao.RoutineDao
import com.luis.itera.data.local.dao.SessionDao
import com.luis.itera.data.local.dao.SetDao
import com.luis.itera.data.local.entity.DailyHydrationGoalEntity
import com.luis.itera.data.local.entity.HydrationIntakeEntity
import com.luis.itera.data.local.entity.RoutineEntity
import com.luis.itera.data.local.entity.RoutineExerciseEntity
import com.luis.itera.data.local.entity.SessionEntity
import com.luis.itera.data.local.entity.SetEntity
import com.luis.itera.domain.model.WorkoutFocus
import java.time.DayOfWeek
import java.time.LocalDate
import kotlin.math.roundToInt
import kotlin.random.Random

/**
 * Semilla de datos de demostración: rutinas, ~8 semanas de sesiones con progresión
 * y PRs, e hidratación reciente. Solo se ejecuta en builds de desarrollo (BuildConfig.DEBUG),
 * enganchada al onCreate de la base de datos, después de [ExerciseSeed].
 */
object DemoSeed {

    private const val WEEKS = 8
    private const val DAY_MILLIS = 86_400_000L

    private data class ExPlan(
        val name: String,
        val sets: Int,
        val repsMin: Int = 0,
        val repsMax: Int = 0,
        val baseWeight: Float = 0f,
        val weeklyStep: Float = 0f,
        val bodyweight: Boolean = false,
        val cardio: Boolean = false,
        val durMinMin: Int = 0,
        val durMinMax: Int = 0
    )

    private data class DayPlan(
        val routine: String,
        val focus: WorkoutFocus,
        val exercises: List<ExPlan>
    )

    private val push = DayPlan(
        routine = "Pecho y tríceps", focus = WorkoutFocus.PUSH,
        exercises = listOf(
            ExPlan("Press banca", 4, 6, 10, baseWeight = 60f, weeklyStep = 1.25f),
            ExPlan("Press banca inclinado con mancuernas", 3, 8, 12, baseWeight = 24f, weeklyStep = 0.5f),
            ExPlan("Press militar con mancuernas", 3, 8, 12, baseWeight = 18f, weeklyStep = 0.5f),
            ExPlan("Fondos en paralelas", 3, 8, 14, bodyweight = true),
            ExPlan("Elevaciones laterales con mancuerna", 3, 12, 15, baseWeight = 10f, weeklyStep = 0.25f),
            ExPlan("Extensión de tríceps en polea", 3, 10, 15, baseWeight = 25f, weeklyStep = 0.5f)
        )
    )

    private val pull = DayPlan(
        routine = "Espalda y bíceps", focus = WorkoutFocus.PULL,
        exercises = listOf(
            ExPlan("Dominadas agarre prono", 4, 6, 10, bodyweight = true),
            ExPlan("Remo con barra", 4, 8, 10, baseWeight = 50f, weeklyStep = 1f),
            ExPlan("Jalón al pecho agarre neutro", 3, 10, 12, baseWeight = 55f, weeklyStep = 1f),
            ExPlan("Curl de bíceps con barra", 3, 8, 12, baseWeight = 25f, weeklyStep = 0.5f),
            ExPlan("Curl martillo", 3, 10, 12, baseWeight = 12f, weeklyStep = 0.25f),
            ExPlan("Pájaros con mancuernas", 3, 12, 15, baseWeight = 8f, weeklyStep = 0.25f)
        )
    )

    private val legs = DayPlan(
        routine = "Pierna completa", focus = WorkoutFocus.LEGS,
        exercises = listOf(
            ExPlan("Sentadilla con barra", 4, 6, 10, baseWeight = 80f, weeklyStep = 2f),
            ExPlan("Prensa de piernas", 4, 10, 12, baseWeight = 140f, weeklyStep = 2.5f),
            ExPlan("Peso muerto rumano con barra", 3, 8, 10, baseWeight = 70f, weeklyStep = 1.5f),
            ExPlan("Extensión de cuádriceps en máquina", 3, 12, 15, baseWeight = 45f, weeklyStep = 1f),
            ExPlan("Curl femoral acostado", 3, 12, 15, baseWeight = 35f, weeklyStep = 1f),
            ExPlan("Costurera", 3, 15, 20, baseWeight = 60f, weeklyStep = 1f)
        )
    )

    private val fullBody = DayPlan(
        routine = "Full body casa", focus = WorkoutFocus.FULL_BODY,
        exercises = listOf(
            ExPlan("Flexiones", 3, 15, 25, bodyweight = true),
            ExPlan("Dominadas agarre prono", 3, 6, 10, bodyweight = true),
            ExPlan("Sentadilla pistol", 3, 5, 8, bodyweight = true),
            ExPlan("Fondos en paralelas", 3, 10, 15, bodyweight = true),
            ExPlan("Flexiones diamante", 3, 12, 18, bodyweight = true)
        )
    )

    private val cardio = DayPlan(
        routine = "Cardio matutino", focus = WorkoutFocus.CARDIO,
        exercises = listOf(
            ExPlan("Carrera continua", 1, cardio = true, durMinMin = 30, durMinMax = 40),
            ExPlan("Saltar cuerda", 3, cardio = true, durMinMin = 4, durMinMax = 6)
        )
    )

    /** Rutinas visibles en la pantalla de Rutinas. */
    private val routines = listOf(push, pull, legs, fullBody, cardio)

    /** Reparto semanal de entrenamientos (miércoles y domingo de descanso). */
    private fun planForDay(date: LocalDate): DayPlan? = when (date.dayOfWeek) {
        DayOfWeek.MONDAY -> push
        DayOfWeek.TUESDAY -> pull
        DayOfWeek.THURSDAY -> legs
        DayOfWeek.FRIDAY -> if (date.toEpochDay() / 7 % 2 == 0L) fullBody else pull
        DayOfWeek.SATURDAY -> cardio
        else -> null
    }

    suspend fun seed(
        exerciseDao: ExerciseDao,
        sessionDao: SessionDao,
        setDao: SetDao,
        routineDao: RoutineDao,
        hydrationDao: HydrationDao
    ) {
        val idByName = exerciseDao.getAllOnce().associate { it.name to it.id }
        fun idOf(name: String): Long? = idByName[name]

        val rng = Random(42)

        seedRoutines(routineDao, ::idOf)
        seedSessions(sessionDao, setDao, ::idOf, rng)
        seedHydration(hydrationDao, rng)
    }

    private suspend fun seedRoutines(routineDao: RoutineDao, idOf: (String) -> Long?) {
        routines.forEach { plan ->
            val routineId = routineDao.insertRoutine(
                RoutineEntity(name = plan.routine, focus = plan.focus.name)
            )
            val items = plan.exercises
                .mapNotNull { ex -> idOf(ex.name) }
                .distinct()
                .mapIndexed { i, exId ->
                    RoutineExerciseEntity(routineId = routineId, exerciseId = exId, displayOrder = i)
                }
            if (items.isNotEmpty()) routineDao.insertRoutineExercises(items)
        }
    }

    private suspend fun seedSessions(
        sessionDao: SessionDao,
        setDao: SetDao,
        idOf: (String) -> Long?,
        rng: Random
    ) {
        val today = LocalDate.now()
        val start = today.minusWeeks(WEEKS.toLong())

        var date = start
        while (date.isBefore(today)) {
            val plan = planForDay(date)
            // ~10% de faltas para que el mapa de actividad no sea perfecto.
            if (plan != null && rng.nextInt(10) != 0) {
                val weekIndex = ((date.toEpochDay() - start.toEpochDay()) / 7).toInt()
                val isRecent = date.isAfter(today.minusDays(10))
                insertSession(sessionDao, setDao, idOf, plan, date, weekIndex, isRecent, rng)
            }
            date = date.plusDays(1)
        }
    }

    private suspend fun insertSession(
        sessionDao: SessionDao,
        setDao: SetDao,
        idOf: (String) -> Long?,
        plan: DayPlan,
        date: LocalDate,
        weekIndex: Int,
        isRecent: Boolean,
        rng: Random
    ) {
        val startMillis = date.toEpochDay() * DAY_MILLIS + 18L * 3_600_000L
        val sessionId = sessionDao.insert(
            SessionEntity(
                dateEpochDay = date.toEpochDay(),
                isFinished = true,
                focus = plan.focus.name,
                startEpochMillis = startMillis
            )
        )

        var order = 1
        var totalSets = 0
        plan.exercises.forEach { ex ->
            val exId = idOf(ex.name) ?: return@forEach
            repeat(ex.sets) { setIdx ->
                val set = if (ex.cardio) {
                    val minutes = rng.nextInt(ex.durMinMin, ex.durMinMax + 1)
                    SetEntity(
                        sessionId = sessionId, exerciseId = exId,
                        reps = 0, weightAddedKg = 0f, order = order,
                        durationSeconds = minutes * 60,
                        intensity = rng.nextInt(6, 10)
                    )
                } else {
                    val reps = rng.nextInt(ex.repsMin, ex.repsMax + 1)
                    val weight = if (ex.bodyweight) 0f
                    else roundToHalf(ex.baseWeight + ex.weeklyStep * weekIndex)
                    // PR: primera serie (la más pesada) de las sesiones recientes.
                    val isPr = isRecent && setIdx == 0
                    SetEntity(
                        sessionId = sessionId, exerciseId = exId,
                        reps = reps, weightAddedKg = weight, order = order,
                        intensity = 0, isPr = isPr
                    )
                }
                order++
                totalSets++
                setDao.insert(set)
            }
        }

        val durationMinutes = if (plan.focus == WorkoutFocus.CARDIO) 40 else totalSets * 4 + 8
        sessionDao.update(
            SessionEntity(
                id = sessionId,
                dateEpochDay = date.toEpochDay(),
                durationMinutes = durationMinutes,
                isFinished = true,
                focus = plan.focus.name,
                startEpochMillis = startMillis
            )
        )
    }

    private suspend fun seedHydration(hydrationDao: HydrationDao, rng: Random) {
        val today = LocalDate.now()
        for (i in 0 until 14) {
            val date = today.minusDays(i.toLong())
            val epochDay = date.toEpochDay()
            val dayStart = epochDay * DAY_MILLIS
            val base = 2500
            val bonus = if (date.dayOfWeek != DayOfWeek.WEDNESDAY && date.dayOfWeek != DayOfWeek.SUNDAY) 400 else 0
            val goal = base + bonus
            hydrationDao.upsertDailyGoal(
                DailyHydrationGoalEntity(
                    dateEpochDay = epochDay,
                    baseGoalMl = base,
                    activityBonusMl = bonus,
                    totalGoalMl = goal,
                    isActiveDay = bonus > 0
                )
            )
            // Ingestas repartidas a lo largo del día, sumando ~cerca del objetivo.
            var consumed = 0
            val target = goal - rng.nextInt(0, 400)
            var hour = 8
            while (consumed < target && hour < 22) {
                val amount = listOf(250, 330, 500).random(rng)
                hydrationDao.insertIntake(
                    HydrationIntakeEntity(
                        dateTimeEpochMillis = dayStart + hour * 3_600_000L,
                        amountMl = amount
                    )
                )
                consumed += amount
                hour += rng.nextInt(1, 3)
            }
        }
    }

    private fun roundToHalf(value: Float): Float = (value / 0.5f).roundToInt() * 0.5f
}
