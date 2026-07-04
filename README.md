# Itera

![Android](https://img.shields.io/badge/Android-3DDC84?style=for-the-badge&logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/kotlin-%237F52FF.svg?style=for-the-badge&logo=kotlin&logoColor=white)
![Jetpack Compose](https://img.shields.io/badge/Jetpack_Compose-4285F4?style=for-the-badge&logo=android&logoColor=white)
![Hilt](https://img.shields.io/badge/Hilt-FF6F00?style=for-the-badge&logo=android&logoColor=white)

## 📌 Descripción

**Itera** es una aplicación Android nativa para registro de entrenamientos de fuerza, calistenia y cardio, con módulo integrado de hidratación diaria. Diseñada bajo la filosofía **offline-first**: cero dependencias de red, cero cuentas, cero servidores. Toda la persistencia es local mediante Room, DataStore y un sistema de navegación de 4 tabs: **Home → Historial → Estadísticas → Hidratación**.

---

## Problema que resuelve

Las apps de fitness populares requieren cuentas, conexión a internet, o monetizan features básicos detrás de paywalls. El usuario que entrena en un gimnasio sin señal, o que simplemente quiere registrar sets sin fricción, queda desatendido.

Itera elimina esa barrera: abres la app, seleccionas tu enfoque del día, registras sets y te vas. Sin onboarding, sin login, sin sync.

---

## Responsabilidades principales

- **Registro de sesiones** con soporte bifurcado para fuerza (reps + peso) y cardio (duración + intensidad)
- **Catálogo de ejercicios** abierto con creación inline y clasificación por 10 grupos musculares
- **Historial** con calendario mensual (kizitonwose/Calendar) y cards agrupadas por día
- **Estadísticas dinámicas** con gráficas Canvas (línea + barras), Top Movimientos por frecuencia y 1RM estimado (Epley)
- **Hidratación** con anillo de progreso angular (arrastre por contorno), meta dinámica por peso corporal, Smart Merge por minuto y eliminación optimista con Snackbar Undo
- **Progresión inline** mostrando los sets de la última sesión para cada ejercicio

---

## Flujo general

```
[ Home ]
  ├── Resumen: fecha, meta semanal, racha, mini anillo hidratación
  ├── Última sesión finalizada (card clickeable → detalle)
  ├── Selector de enfoque: PUSH | PULL | LEGS | UPPER | LOWER | FULL BODY | CARDIO
  └── INICIAR ENTRENAMIENTO (deshabilitado sin selección)
        │
        ▼
[ Sesión Activa ]
  ├── Buscador colapsable con lupa animada
  ├── Ejercicios filtrados por enfoque seleccionado
  ├── FastStepper (±1 tap, ±5 long press, input manual con auto-reversión)
  │     ├── Fuerza: REPS / +KG
  │     └── Cardio: MINUTOS / NIVEL
  ├── Registrar set → feedback háptico + confirmación visual
  └── Finalizar → descarta si 0 sets, navega a detalle si tiene sets
        │
        ▼
[ Historial ]                    [ Estadísticas ]
  ├── Calendario mensual           ├── Métricas: sesiones/mes, focus top, racha
  ├── Cards por día (.take(3))     ├── Top Movimientos (3 más frecuentes)
  └── Tap → SessionDetail          ├── Selector ejercicio + chips grupo muscular
                                   ├── StatLineChart (PR sólido, clipRect animado)
                                   └── StatBarChart (scroll horizontal, grid punteado)
```

---

## Modelo de datos

**Room v4** — 4 entidades principales:

```
sessions
  ├── id (PK, autoGenerate)
  ├── dateEpochDay (Long)
  ├── isFinished (Boolean)
  ├── focus (String? — CSV de WorkoutFocus)
  ├── startEpochMillis (Long)
  └── durationMinutes (Int)

sets
  ├── id (PK, autoGenerate)
  ├── sessionId (FK → sessions, CASCADE)
  ├── exerciseId (FK → exercises, RESTRICT)
  ├── reps (Int)
  ├── weightAddedKg (Float)
  ├── order (Int)
  ├── durationSeconds (Int)      ← cardio
  └── intensity (Int)            ← cardio

exercises
  ├── id (PK, autoGenerate)
  ├── name (String, UNIQUE)
  ├── category (String)
  ├── equipment (String)
  └── mainMuscleGroup (String)

hydration_intakes
  ├── id (PK, autoGenerate)
  ├── dateTimeEpochMillis (Long)
  └── amountMl (Int)
```

**DataStore** — preferencias clave-valor: `weeklyGoal (Int)`, `userWeightKg (Float)`.

**Migraciones**: `v1 → v2` (startEpochMillis), `v2 → v3` (hydration tables), `v3 → v4` (durationSeconds + intensity en sets).

---

## Decisiones técnicas

- **Combine de 6+ Flows en ViewModels** con `Array<Any?>` casts y data classes intermedias (`InputBundle`) para evitar el límite de 5 parámetros de `combine()`.
- **Eliminación optimista** en hidratación: `pendingDeletionIds` en memoria, filtrado en el `combine`, commit diferido al expirar Snackbar o en `onCleared()`.
- **Smart Merge**: si dos registros de agua caen en el mismo minuto, se ejecuta UPDATE en vez de INSERT.
- **Reactividad total**: `getMostTrainedExerciseId()` como `Flow<Long?>` (no suspend) para que las estadísticas se limpien instantáneamente al vaciar la BD.
- **FastStepper stateless** con patrón optimista (`optimisticValue`) y rastreador `hasGainedFocus` para evitar falso positivo de `onFocusChanged` durante composición inicial.
- **Insets unificados**: `consumeWindowInsets(paddingValues)` en NavHost para prevenir doble padding del teclado con la barra de navegación.
- **Bifurcación cardio/fuerza**: condicional en UI (steppers), registro (campos distintos en SetEntity), estadísticas (duración máxima vs 1RM) y detalle de sesión.

---

## Desarrollo local

### Prerrequisitos

- Android Studio Ladybug (2024.2+)
- JDK 17
- Dispositivo físico o emulador API 29+

### Clonar y ejecutar

```bash
git clone https://github.com/MrX-zeta/Itera.git
cd Itera
git checkout development
```

1. Abrir en Android Studio → esperar sync de Gradle.
2. Conectar dispositivo con opciones de desarrollador habilitadas.
3. **Run (Shift + F10)**.

No requiere claves de API, archivos de configuración externos ni conexión a internet.

---

## Relación con el sistema

Itera es un proyecto **standalone** sin backend ni microservicios. Todo el valor técnico está en la capa de presentación y persistencia local:

- **Compose puro** — cero XML, cero Fragments, cero Activities secundarias.
- **Canvas custom** — gráficas de línea y barras dibujadas a mano con animaciones `clipRect` y `Animatable`.
- **Gestos avanzados** — anillo de hidratación con arrastre angular (`atan2`), swipe-to-delete con `SwipeToDismissBox`, long press en steppers.
- **Room reactivo** — todos los DAOs exponen `Flow<T>`, la UI nunca hace polling.
- **Hilt** — inyección en ViewModels, repositorios, use cases y módulo de BD con migraciones.
