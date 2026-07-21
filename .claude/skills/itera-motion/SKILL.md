---
name: itera-motion
description: >
  Principios de motion, dirección visual y convenciones de UI/UX para Itera (registro de entrenamiento
  de fuerza/calistenia/cardio + hidratación, Android nativo, offline-first). Aplica esta skill SIEMPRE
  que trabajes en Itera: cualquier pantalla, componente, transición, gesto, gráfica, heatmap, widget,
  ajustes o elemento visual. Codifica los principios de Emil Kowalski adaptados a Itera, la paleta teal
  madura, el sistema de acento dinámico, las decisiones del refactor de UI/UX, y la lista de intocables.
---

# Itera · Motion, dirección visual y convenciones

> Skill ESPECÍFICA de Itera. Vive en el repo (`.claude/skills/itera-motion/`). Las skills genéricas de
> animación de Emil viven aparte, en `~/.claude/skills/` (globales): `emil-design-eng`,
> `review-animations`, `improve-animations`, `animation-vocabulary`, `apple-design`. Esta skill las
> COMPLEMENTA con lo propio de Itera; no las reemplaza.

## Qué es Itera (contexto)
App Android nativa, Kotlin + Jetpack Compose (Compose puro, cero XML/Fragments/Activities secundarias),
offline-first (sin cuentas, sin red, sin APIs). Registro de entrenamiento de fuerza, calistenia y cardio
+ módulo de hidratación. MVVM + Clean, Hilt, Room, DataStore. 4 pestañas: Entrenamiento (Home) ·
Historial · Estadísticas · Hidratación. Widget Glance de pantalla de inicio.

Se está haciendo un REFACTOR de UI/UX sobre el código existente (NO un rewrite). La lógica madura se
conserva; se rehace la capa de presentación y se añaden features acordadas. Se PUEDE tocar lógica cuando
la feature lo exige (permiso dado), salvo lo marcado como INTOCABLE (ver la sección final).

## Principios de motion (Emil Kowalski, adaptados a Compose)
La animación sirve a la COMPRENSIÓN, no al adorno:
- Toda animación tiene un PROPÓSITO: guiar la atención, dar continuidad espacial, confirmar una acción.
  Si no ayuda a entender qué pasó, sobra.
- Movimiento NATURAL: curvas físicas (spring/ease), nunca lineales para movimiento perceptible. Sin
  rebotes gratuitos; asentamiento calmado (dampingRatio alto).
- Duraciones cortas y honestas: microinteracciones ~150-250 ms; transiciones de pantalla algo más.
- Respeta SIEMPRE el movimiento reducido del sistema: degrada a cambios directos/fades.
- El motion no bloquea la interacción ni retrasa el feedback: el usuario manda, la animación acompaña.
- Coherencia: el mismo tipo de elemento se anima igual en toda la app.

## Filosofía visual del refactor ("marlune-ficar")
El norte es CONTENCIÓN. El problema de la Itera vieja era ruido visual (bordes por todos lados, acento
repartido, todo compitiendo con el mismo peso). El refactor QUITA, no añade:
- SIN bordes por defecto. Separa con SUPERFICIE ELEVADA (un tono más claro que el fondo), no con contorno.
- Acento CONTENIDO: una sola cosa importante en acento por zona. Los números/datos normales van en texto
  neutro; el acento se reserva para lo activo/seleccionado y lo interactivo primario.
- AIRE: espaciado generoso entre secciones. Nada pegado a los bordes.
- JERARQUÍA tipográfica: el dato importante grande, lo secundario pequeño y gris. Encabezados de sección
  en mayúsculas pequeñas y color tenue.
- LEGIBILIDAD DE FUENTE (regla explícita): NO tengas miedo de aumentar el tamaño de los textos. El error
  típico es dejar los textos SECUNDARIOS (subtítulos, metadatos, descripciones, valores bajo una etiqueta,
  texto de apoyo) demasiado pequeños. La jerarquía se logra con el CONTRASTE entre tamaños y con el color
  (secundario/terciario), NO haciendo diminuto el texto secundario. Un subtítulo o metadato debe leerse
  cómodamente en el dispositivo: parte de ~13-14sp para secundarios y ~15-16sp para cuerpo, y súbelo si
  queda justo, en vez de encogerlo. Solo los encabezados de sección tipo etiqueta (mayúsculas) y micro-
  leyendas pueden ir pequeños (~11-12sp). Ante la duda entre "un poco más grande" y "un poco más pequeño",
  elige MÁS GRANDE. La legibilidad manda sobre la compacidad.
- Oscuro CON claridad: fondo no-negro-puro, superficies elevadas perceptibles, sin bordes decorativos.

## PALETA — Teal maduro (fuente de verdad, no inventar colores)
Acento por DEFECTO teal maduro (menos neón que el aqua viejo). Oscuro con claridad.
- Fondo:                 #0F1113
- Superficie:            #16191D
- Superficie elevada:    #1E2229   (para controles/cuadros dentro de tarjetas)
- Texto primario:        #EDEEF0
- Texto secundario:      #8A8D94
- Texto terciario:       #6E7178   (encabezados de sección, hints)
- Acento (teal):         #2BBFA8
- Acento tenue:          #176B5C   (estados intermedios, heatmap nivel medio)
- Sobre acento:          #04302A   (texto/icono sobre relleno de acento)
- PROGRESO / LOGRO (ámbar): #E8B75D  — REGLA ESTRICTA: SOLO para logros (PR, "+1 rep vs antes", 110% de
  hidratación, récord). NUNCA como marca, ni relleno de botón, ni acento primario. Verde=actividad,
  ámbar=logro: son idiomas distintos y no se mezclan.
- Azul-agua (hidratación): #5FC5DB / medio #3F9BB8 / tenue #26525E — EXCLUSIVO del heatmap e histórico de
  hidratación, para distinguir el hábito de agua del de entrenamiento (que va en teal). No usar el azul
  fuera de hidratación.
- Error: #E24B4A

## Sistema de ACENTO DINÁMICO (infraestructura, hacer PRIMERO)
El usuario elige el acento en Ajustes entre 3 curados: Teal #2BBFA8 (default) · Índigo #7C6FF0 ·
Lima #9BE23F. El selector cambia SOLO el acento; fondos, superficies y texto se mantienen neutros
SIEMPRE (ninguna combinación puede volverse ilegible).
Estado del código HOY (verificado): `IteraColors` es un `object` con constantes; las pantallas consumen
`IteraColors.Accent` directamente (~74 referencias), NO `MaterialTheme.colorScheme.primary`. Como es un
`val`, cambiarlo en runtime no recompone.
Para el acento reactivo: migrar a un CompositionLocal (o alimentar colorScheme.primary desde DataStore) y
migrar las referencias directas. Nueva clave en Preferences DataStore: `accent_color`. Como el refactor
toca todas las pantallas, la migración de referencias se hace de paso en cada una. Los neutros siguen en
`IteraColors`; SOLO el acento pasa a ser dinámico.

## Componentes y patrones (estado real del código, para no romper)
- FastStepper: composable reutilizable aislado (FastStepper.kt), usado en 6+ sitios (reps, +kg, minutos,
  nivel, hidratación, meta, peso). Reestilizar en un solo lugar. OJO: el teclado depende de
  `if (label == "REPS")` — acoplamiento por string, no romperlo al reestilizar.
- Celebración de PR: se CONSERVA. isPr se decide en el ViewModel (onRegisterSet); confetti en
  ConfettiOverlay.kt; fuego+háptico en RegisterSetButton. La "vibración" es haptic feedback de Compose
  (performHapticFeedback), NO Vibrator del sistema (no hay permiso VIBRATE). Si el descanso necesita
  aviso al llegar a 0, usar haptic feedback, no introducir Vibrator salvo que se decida explícitamente.
- Navegación: NavHost con `sealed class IteraDestination(val route)`. Pestañas: active_workout, history,
  statistics, hydration. Extra: onboarding, session_detail/{id}. Para AJUSTES: añadir
  `Settings : IteraDestination("settings")`, un composable en el NavHost, y navegar desde un icono (NO
  como 5ª pestaña); revisar showBottomBar/ownsRoute como hace session_detail.

## Decisiones del refactor (qué construir, resumen)
- HOME: sin huecos. "Tus rutinas" sube al cuerpo como tarjetas 2-up con icono; racha+meta fusionadas en
  una línea; barra de progreso semanal (7 segmentos) en vez de 7 puntos; anillo de hidratación con % dentro.
- SESIÓN ACTIVA (la más usada, cómoda con el pulgar): steppers grandes; sugerencia "última vez" arriba;
  sets de hoy debajo con logro en ámbar ("+1 rep vs antes").
- DESCANSO: hoy cuenta hacia ARRIBA sin meta (SessionTimer.kt, estado en ActiveWorkoutViewModel). CAMBIAR
  a CUENTA ATRÁS con meta configurable (default global ~90s, editable en Ajustes), con "saltar" y "+30s",
  y aviso (haptic) al llegar a 0. Sugiere, no obliga.
- INTEGRIDAD DE SETS: el embudo es `addSet` (SessionRepositoryImpl), hoy sin validación. Añadir: si el
  ejercicio requiere carga externa —`equipment ∈ {Barra, Mancuerna, Máquina, Polea}` Y
  `category ≠ "Calistenia"`— y el peso es 0 → AVISO suave confirmable ("¿registrar sin peso?"), NO bloqueo
  (la barra de dominadas/muscle up es equipment=Barra pero category=Calistenia → NO pide peso). Reps en 0
  → SÍ bloquear (no tiene lectura legítima). Peso corporal → 0 kg válido y silencioso.
- HEATMAP (Historial): pasar de "brillo por nº de grupos musculares" a CONSTANCIA BINARIA. Reutilizar
  `getAllTrainedDays()` (ya existe) y ELIMINAR `getDailyMuscleGroupCount()`; data pasa de Map<LocalDate,Int>
  a Set<LocalDate>; colapsar levelFor/ALPHA_BY_LEVEL a 2 estados (entrenó / no). Un día entrenado ilumina
  en teal parejo (da igual fuerza/calistenia/cardio; todos cuentan igual). Los días con PR llevan un punto
  ÁMBAR (no más brillo: otro color, no otra intensidad). Hoy = casilla con contorno. Nada de nº de sesiones.
- "¿DÓNDE ME QUEDÉ?": poder consultar la última sesión de un ejercicio ANTES de entrenar (fuera de sesión
  activa), desde Historial o el detalle de ejercicio. Cierra un hueco de UX.
- HIDRATACIÓN: toggle "Hoy / Historial" dentro de la misma pestaña (sin navegación nueva). El histórico:
  heatmap AZUL-AGUA de cumplimiento de meta + lista por día (total, nº de registros, % de meta). El DAO ya
  tiene getIntakesBetween/getTotalMlBetween; falta getDailyGoalsBetween(from,to) y replicar el cálculo de
  meta al vuelo para días sin fila (como hace WidgetDataLoader). Es un heatmap DISTINTO del de
  entrenamiento (otro hábito, otro color); NUNCA se ven en la misma pantalla, así que no hay redundancia.
- AJUSTES (pantalla NUEVA): Apariencia (selector de acento, 3 muestras) · Widget (anclar con idempotencia
  + toast "El widget ya está en la pantalla de inicio", como en Tramo: getAppWidgetIds antes de
  requestPinAppWidget) · Perfil (peso, meta semanal, acerca de). También los defaults configurables:
  descanso por defecto (~90s), y umbrales de pestañas dinámicas (4 semanas / 3 sesiones).
- ESTADÍSTICAS / MÉTRICAS (la más pesada, va AL FINAL): filosofía = responder "¿progresé?" en lenguaje
  humano ("Vas mejorando", "subió 8%", "8→12 reps"), NUNCA cifra cruda sin referente (nada de "30.3 ton").
  Comparación vs antes, no valor absoluto.
  · Vista ADAPTATIVA por perfil con filtro: General (fija, siempre) + Fuerza/Calistenia/Cardio.
  · Pestañas de modalidad DINÁMICAS: aparecen solo si RECIENTE (≤4 semanas, ventana móvil) Y SIGNIFICATIVO
    (≥3 sesiones). Un día de calistenia puntual NUNCA crea pestaña; una modalidad pausada 3 meses se OCULTA
    del foco. Umbrales configurables en Ajustes.
  · General: titular "¿progresé?" ("Vas mejorando") + constancia (racha, días) + equilibrio ("esto
    descuidas", en ámbar). Usuario nuevo sin datos: solo General con estado vacío que invita.
  · Fuerza: progresión por ejercicio (kg, gráfica) + "fuerza estimada +X%" (1RM traducido) + top
    movimientos (↑ mejorando / → estable). 1RM ABSOLUTO solo en los 3 básicos (sentadilla, press banca,
    peso muerto) donde el número significa algo; en aislamientos SOLO el %, nunca 1RM crudo.
  · Calistenia: reps y dificultad (sin kg). Cardio: duración e intensidad.
  · NOTA: hoy NO existe clasificación por modalidad (solo `category` String: "Gimnasio"/"Calistenia"/
    "Cardio"/"Personalizado"), NO existe 1RM (hay que crearlo), NO existe "última sesión por modalidad"
    (focus es CSV multivalor; parsear en Kotlin). Mapear "Gimnasio"→Fuerza. Es la pantalla con MÁS lógica
    nueva; por eso va al final.

## ARCHIVADO vs HISTORIAL (concepto clave)
"Archivar" una modalidad = quitarla del FOCO de Estadísticas (pestaña dinámica), NUNCA borrar ni ocultar
datos. HISTORIAL conserva TODO, siempre consultable (el día de calistenia puntual y los meses de gym
pausado siguen ahí completos). La puerta a los datos siempre está abierta en Historial.

## INTOCABLE (no modificar salvo que la feature lo exija Y se confirme)
- `data/` completo: Room v7 (IteraDatabase, version=7) + las 6 migraciones (MIGRATION_1_2..6_7 en
  DatabaseModule). NO tocar entidades, DAOs ni migraciones salvo añadir queries nuevas (p.ej.
  getDailyGoalsBetween) sin romper las existentes; jamás cambiar el esquema sin migración.
- Lógica de hidratación: Smart Merge (UPDATE si misma hora/minuto), borrado optimista con Snackbar Undo,
  cálculo de meta por peso.
- Doble progresión (sube reps hasta techo, luego peso), detección de PR, dedup de rutinas por contenido,
  proyección de tendencia (RISING/STABLE/FALLING/DELOAD), getLastExercisedId.
- Ajustes de sincronización del widget (el canal reactivo WidgetEntryPoint→repos→combine→collectAsState).
- durationMinutes se MIDE (reloj de pared en finishSession), no se toca.

## Reglas de trabajo (SIEMPRE)
- NO rompas lo que ya está definido y funciona. Si un cambio pareciera tocar algo INTOCABLE, PÁRATE y
  pregunta.
- Cambios acotados a la tarea pedida; nada de refactors no solicitados dentro de un cambio.
- Respuestas BREVES.
- Control de versiones: NO ejecutar git. Entrega los comandos de commit en el chat, ATÓMICOS, en español,
  convencionales (feat/fix/style/refactor), SIN co-autoría, SIN "Generated with Claude Code", SIN push.
  Los commits se hacen AL FINAL, solo cuando el usuario confirme que el cambio quedó bien.
- Ciclo de prueba en CADA cambio relevante: desinstalar el APK actual del dispositivo, compilar,
  reinstalar el APK de desarrollo, y verificar en el dispositivo antes de dar el cambio por bueno.
- Las maquetas HTML que originaron este diseño son el PLANO (estructura, jerarquía, color), no el
  pixel-perfect: el render real es Compose con los componentes de Itera.