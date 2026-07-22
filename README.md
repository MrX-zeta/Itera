# Itera

### App Android nativa de entrenamiento — 100% Jetpack Compose, 100% offline, 0 cuentas, 0 servidores.

![Android](https://img.shields.io/badge/Android-3DDC84?style=for-the-badge&logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/kotlin-%237F52FF.svg?style=for-the-badge&logo=kotlin&logoColor=white)
![Jetpack Compose](https://img.shields.io/badge/Jetpack_Compose-4285F4?style=for-the-badge&logo=android&logoColor=white)
![Hilt](https://img.shields.io/badge/Hilt-FF6F00?style=for-the-badge&logo=android&logoColor=white)
![Room](https://img.shields.io/badge/Room-DB-4A90D9?style=for-the-badge)
![minSdk 26](https://img.shields.io/badge/minSdk-26-success?style=for-the-badge)

Registra entrenamientos de fuerza, calistenia y cardio, con hidratación integrada. Sin internet, sin login, sin backend: todo vive en el teléfono. **[⬇ Descargar el APK de la última versión](https://github.com/MrX-zeta/Itera/releases/latest)**

---

## Problemática a resolver

Registrar un entrenamiento no debería requerir wifi, una cuenta ni un paywall. Itera hace exactamente eso y nada más: eliges tu enfoque, registras series, y la app te sugiere tu siguiente progresión — todo local, todo instantáneo. Los únicos datos que se piden (peso, meta semanal) se preguntan en su contexto real, la primera vez que hacen falta, nunca en un formulario de bienvenida.

## Lo que este proyecto demuestra

- **Producto completo de punta a punta**: de un MVP a una v2.0 con refactor integral de UI/UX, release firmado con R8/ProGuard, y publicación real en GitHub Releases — no es un tutorial ni un CRUD de ejemplo.
- **Arquitectura de producción**: MVVM + Clean Architecture, Room con 8 versiones de esquema y 7 migraciones sin pérdida de datos, inyección de dependencias con Hilt, `Flow` de punta a punta (la UI nunca hace polling).
- **UI 100% Compose, sin una línea de XML**: animaciones, gestos y gráficas (heatmaps, líneas, barras) dibujados a mano sobre `Canvas`, con un sistema de acento dinámico reactivo en toda la app.
- **Más allá de la actividad principal**: un widget de pantalla de inicio (Glance) con su propio canal reactivo hacia Room, funcionando en 4 tamaños.
- **Higiene de release real**: reglas de ofuscación explícitas y justificadas, `versionCode`/firma que fallan ruidosamente si falta algo, y una app que arranca genuinamente virgen para cada usuario nuevo (sin datos de prueba filtrados, sin restauración automática indebida).

---

## Responsabilidades principales

- **Registro de sesiones** con soporte bifurcado para fuerza (reps + peso) y cardio (duración + intensidad), doble progresión automática (sube reps hasta un techo, luego peso) y detección de PR con celebración (confeti + háptico)
- **Rutinas**: gestión completa (crear, editar, colorear con una paleta de 10 tonos), inicio directo de sesión desde la lista o desde Entrenamiento
- **Catálogo de ejercicios** abierto con creación inline y clasificación por 11 grupos musculares (incluye Cardio)
- **Descanso en cuenta atrás** entre series, con meta configurable en Ajustes y "+30 s" puntual sin alterar el default
- **Historial** con mapa de actividad (heatmap de 10 semanas, constancia binaria + punto ámbar de PR) y "¿dónde me quedé?" para consultar la última sesión de un ejercicio antes de entrenar
- **Estadísticas adaptativas** por modalidad (General siempre, Fuerza/Calistenia/Cardio solo si hay actividad reciente y significativa): 1RM estimado (Epley), variación de fuerza, Top Movimientos, y una card "¿Progresé?" cuya evidencia nunca contradice al titular
- **Hidratación** con anillo de progreso angular (arrastre por contorno), meta dinámica por peso corporal, Smart Merge por minuto, histórico con heatmap propio y eliminación optimista con Snackbar Undo
- **Widget** de pantalla de inicio (Glance) en 4 tamaños con layout propio, semana de 7 días siempre completa y el mismo acento dinámico de la app
- **Ajustes**: acento (Teal / Índigo / Lima) reactivo en toda la app, anclar widget, peso, meta semanal, descanso por defecto

---

## Flujo general

```
[ Onboarding — 3 pantallas ]
  └── Saltar / Empezar → marca onboarding_completed, sin pedir datos
        │
        ▼
[ Entrenamiento ]  ←→(gestos)→  [ Rutinas ]  ←→  [ Historial ]  ←→  [ Estadísticas ]  ←→  [ Hidratación ]
  ├── Resumen: fecha, meta semanal,        ├── Mapa de actividad         ├── General: ¿Progresé?,      ├── Anillo de progreso
  │     racha, mini anillo hidratación     │     (heatmap 10 sem)        │     Constancia, Esto         │     (arrastre angular)
  ├── Última sesión (card → detalle)       ├── Cards por día             │     descuidas                ├── Toggle Hoy / Historial
  ├── Tus rutinas (play directo)           └── Tap → SessionDetail       ├── Fuerza / Calistenia /      └── Smart Merge + Undo
  └── INICIAR ENTRENAMIENTO                                              │     Cardio (dinámicas)
        │            └── abre selector de foco (modal)                  └── 1RM, Top Movimientos
        ▼
[ Sesión Activa ]
  ├── Buscador colapsable + rutina cargada opcional
  ├── FastStepper (±1 tap, ±5 long press, input manual con auto-reversión)
  ├── Descanso en cuenta atrás entre series (+30 s puntual)
  ├── Registrar set → háptico + PR → confeti
  └── Finalizar → detalle · Descartar → Home (flecha y gesto de retroceso, mismo comportamiento)

[ Ajustes ]  (icono, no es pestaña)          [ Widget — pantalla de inicio ]
  └── Acento, widget, peso, meta, descanso     └── 2×1 / 2×2 / 4×1 / 4×2, acento dinámico
```

---

## Modelo de datos

**Room v8** — 7 entidades:

```
sessions
  ├── id (PK, autoGenerate)
  ├── dateEpochDay (Long)
  ├── isFinished (Boolean)
  ├── focus (String? — CSV de WorkoutFocus)
  ├── startEpochMillis (Long)
  └── durationMinutes (Int)              ← medido con reloj de pared, nunca calculado

sets
  ├── id (PK, autoGenerate)
  ├── sessionId (FK → sessions, CASCADE)
  ├── exerciseId (FK → exercises, RESTRICT)
  ├── reps (Int)
  ├── weightAddedKg (Float)
  ├── order (Int)
  ├── durationSeconds / intensity        ← cardio
  ├── workSeconds / restSeconds
  └── isPr (Boolean)

exercises
  ├── id (PK, autoGenerate)
  ├── name (String, UNIQUE)
  ├── category / equipment / mainMuscleGroup

routines
  ├── id (PK, autoGenerate)
  ├── name (String)
  ├── focus (String?)
  └── color (Int — ordinal de RoutineColor)

routine_exercises
  ├── id (PK, autoGenerate)
  ├── routineId (FK → routines, CASCADE)
  ├── exerciseId (FK → exercises, CASCADE)
  └── displayOrder (Int)

hydration_intakes
  ├── id (PK, autoGenerate)
  ├── dateTimeEpochMillis (Long)
  └── amountMl (Int)

daily_hydration_goals
  ├── dateEpochDay (PK)
  ├── baseGoalMl / activityBonusMl / totalGoalMl
  └── isActiveDay (Boolean)
```

**DataStore** — preferencias clave-valor: `weekly_goal`, `user_weight_kg`, `weight_prompt_dismissed`, `onboarding_completed`, `accent_color`, `rest_goal_seconds`, `stats_recent_weeks`, `stats_min_sessions`.

**Migraciones**: `1→2` (focus en sessions) · `2→3` (startEpochMillis) · `3→4` (duración/intensidad en sets, cardio) · `4→5` (workSeconds/restSeconds) · `5→6` (isPr) · `6→7` (tablas de rutinas) · `7→8` (color de rutina).

---

## Decisiones técnicas

- **Acento dinámico reactivo**: `CompositionLocal` (`LocalAccent`) alimentado desde DataStore vía Hilt; los neutros (fondo, superficie, texto) son constantes fijas que nunca cambian con el selector — ninguna combinación puede volverse ilegible.
- **Regla de color estricta**: acento = actividad/interactivo; ámbar = exclusivo de logros (PR, récord); nunca se usan indistintamente. El resaltado de "mejora" en Estadísticas exige comparación **estrictamente positiva** (`>`, no `>=`): un empate no se celebra.
- **Navegación por gestos**: un único `HorizontalPager` aloja las 5 pestañas (`beyondViewportPageCount` para mantenerlas todas compuestas y evitar recompose en frío al volver). La barra inferior nunca se dispone/reconstruye: anima alto/alpha/traslación con `graphicsLayer`, sincronizada en duración con el fundido del `NavHost`.
- **Combine de 6+ Flows en ViewModels** con la sobrecarga `Array<Any?>` para evitar el límite de 5 parámetros de `combine()`.
- **Eliminación optimista** en hidratación: `pendingDeletionIds` en memoria, filtrado en el `combine`, commit diferido al expirar el Snackbar o en `onCleared()`.
- **Smart Merge**: si dos registros de agua caen en el mismo minuto, se ejecuta UPDATE en vez de INSERT.
- **Heatmap compartido**: un solo componente genérico (`level: Int` + color inyectado) para el mapa de entrenamiento (constancia binaria + PR ámbar) y el de hidratación (cumplimiento de meta) — mismo grid, misma animación de barrido, semántica de color propia por pantalla.
- **Widget con Glance**: 4 tamaños fijos vía `SizeMode.Responsive`, cada uno con su propio layout (nunca datos a medias); reglas R8 explícitas para el `GlanceAppWidget`/`GlanceAppWidgetReceiver`, sospechosos número uno de crash en release.
- **`allowBackup="false"`**: app offline-first sin concepto de sync — el respaldo automático del sistema podía restaurar una base de datos vieja al reinstalar, algo inaceptable para quien recibe la app de cero.
- **FastStepper stateless** con patrón optimista (`optimisticValue`) y rastreador `hasGainedFocus` para evitar falso positivo de `onFocusChanged` durante composición inicial.
- **Bifurcación cardio/fuerza**: condicional en UI (steppers), registro (campos distintos en `SetEntity`), estadísticas (duración máxima vs 1RM) y detalle de sesión.

---

## Desarrollo local

### Prerrequisitos

- Android Studio Ladybug (2024.2+) o más reciente
- JDK 17
- Dispositivo físico o emulador API 26+ (widget requiere API 26+ para Glance)

### Clonar y ejecutar

```bash
git clone https://github.com/MrX-zeta/Itera.git
cd Itera
git checkout development
```

1. Abrir en Android Studio → esperar sync de Gradle.
2. Conectar dispositivo con opciones de desarrollador habilitadas.
3. **Run (Shift + F10)**.

No requiere claves de API, archivos de configuración externos ni conexión a internet. El build de `debug` siembra datos de ejemplo (`DemoSeed`, ~8 semanas de historial) para probar cómodo; el build de `release` nunca los incluye.

---

## Relación con el sistema

Itera es un proyecto **standalone** sin backend ni microservicios. Todo el valor técnico está en la capa de presentación y persistencia local:

- **Compose puro** — cero XML, cero Fragments, cero Activities secundarias.
- **Canvas custom** — gráficas de línea y barras, heatmaps y la franja de acento de las rutinas, dibujados a mano con animaciones `clipRect`/`graphicsLayer` y `Animatable`.
- **Gestos avanzados** — anillo de hidratación con arrastre angular (`atan2`), swipe-to-delete con `SwipeToDismissBox`, navegación entre pestañas por `HorizontalPager`.
- **Room reactivo** — todos los DAOs exponen `Flow<T>`, la UI nunca hace polling.
- **Hilt** — inyección en ViewModels, repositorios, use cases, módulo de BD con migraciones y el `EntryPoint` que alimenta al widget.
- **Glance** — widget de pantalla de inicio, con su propio canal reactivo (Room → repos → `combine` → `collectAsState`) independiente de la actividad principal.
