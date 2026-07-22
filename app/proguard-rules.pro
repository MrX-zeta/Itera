# Itera — reglas R8 para el build de release.
# Cada bloque está justificado: solo se guarda lo que de verdad se instancia o busca por
# nombre/reflexión fuera del grafo de referencias directas que R8 ya sigue.

# ---------------------------------------------------------------------------
# GLANCE (widget) — sospechoso nº1 de crash en release.
# IteraWidgetReceiver ya está declarado en el manifest (AGP lo mantiene solo), pero lo
# dejamos explícito por claridad. IteraWidget NO está en el manifest: solo se referencia
# desde código Kotlin directo (`IteraWidgetReceiver.glanceAppWidget = IteraWidget()`), así
# que R8 ya lo sigue — pero Glance internamente reconstruye el árbol de RemoteViews vía
# reflexión sobre las clases GlanceAppWidget/GlanceAppWidgetReceiver del propio proceso al
# recomponer tras el reinicio de la app; sin este keep, ofuscar sus miembros rompe esa
# reconstrucción en release (nunca se ve en debug, que no minifica).
-keep class com.luis.itera.presentation.widget.IteraWidget { *; }
-keep class com.luis.itera.presentation.widget.IteraWidgetReceiver { *; }
-keep class * extends androidx.glance.appwidget.GlanceAppWidget { *; }
-keep class * extends androidx.glance.appwidget.GlanceAppWidgetReceiver { *; }
# Por si en el futuro se añade una acción custom (hoy solo se usa actionStartActivity, que
# no la necesita): Glance invoca los ActionCallback por reflexión desde el Intent guardado.
-keep class * implements androidx.glance.appwidget.action.ActionCallback { *; }

# ---------------------------------------------------------------------------
# ROOM — entidades, DAOs, generado por KSP y MIGRACIONES.
# Room.databaseBuilder() carga la clase generada "IteraDatabase_Impl" por convención de
# nombre (reflexión), no por referencia directa: si R8 la renombra, el build de release
# revienta al abrir la BD la primera vez. Las migraciones SÍ se referencian directo en
# DatabaseModule (.addMigrations(...)), así que R8 ya las sigue — pero una migración
# eliminada por error es irreparable para quien actualice con datos reales, así que se
# guardan explícitas como red de seguridad, no por necesidad estricta de R8.
-keep class com.luis.itera.data.local.IteraDatabase { *; }
-keep class com.luis.itera.data.local.IteraDatabase_Impl { *; }
-keep class * extends androidx.room.RoomDatabase { *; }
-keep interface com.luis.itera.data.local.dao.** { *; }
-keep class com.luis.itera.data.local.dao.**_Impl { *; }
-keep class * extends androidx.room.migration.Migration { *; }
-keep @androidx.room.Entity class * { *; }

# ---------------------------------------------------------------------------
# HILT / DAGGER — generado 100% en compilación (sin reflexión), pero el punto de entrada
# (IteraApp, Activities @AndroidEntryPoint) debe conservar su jerarquía intacta para que
# los Hilt_* generados encajen encima.
-keep class com.luis.itera.IteraApp { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager { *; }
-keep,allowobfuscation @dagger.hilt.android.HiltAndroidApp class *
-keep,allowobfuscation @dagger.hilt.android.AndroidEntryPoint class *

# ---------------------------------------------------------------------------
# DATASTORE PREFERENCES — riesgo bajo: las claves son typed (booleanPreferencesKey,
# intPreferencesKey, floatPreferencesKey), sin serialización por reflexión. Se guarda igual
# como red de seguridad ante cualquier acceso interno por nombre de campo.
-keep class androidx.datastore.*.** { *; }

# ---------------------------------------------------------------------------
# KOTLIN COROUTINES / METADATA — evita warnings de R8 por clases opcionales del runtime de
# corrutinas que no están en el classpath de Android (no son código propio, es limpieza).
-dontwarn kotlinx.coroutines.debug.**
-dontwarn org.jetbrains.annotations.**
