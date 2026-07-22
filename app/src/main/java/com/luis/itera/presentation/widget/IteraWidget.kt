package com.luis.itera.presentation.widget

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.luis.itera.MainActivity
import com.luis.itera.R
import com.luis.itera.presentation.theme.AccentColor

// NEUTROS del widget: fijos siempre (regla de la skill). Solo el ACENTO es
// dinámico y viaja en WidgetData.accent desde la preferencia de Ajustes.
private val CardBg = Color(0xFF141418)
private val TextPrimary = Color(0xFFE8E8EA)
private val TextSecondary = Color(0xFF8A8A92)
private val TitleColor = Color(0xFF9E9EA6)
// Un paso más claro que la "superficie elevada" de la app (#1E2229): sobre el
// fondo de la tarjeta del widget esa apenas se distinguía; los días vacíos
// deben leerse como huecos por llenar, no fundirse con el fondo.
private val SegmentEmpty = Color(0xFF3A3A44)
private val RingTrack = Color(0xFF33333B)

private val DAY_INITIALS = listOf("L", "M", "M", "J", "V", "S", "D")

// Escalera de CUATRO tamaños, cada uno con layout propio; fuera de este set no
// se ofrece nada (el cuadrado grande vacío se eliminó: si no se puede llenar
// con contenido honesto, no va).
private val SizeA = DpSize(140.dp, 48.dp)   // 2×1: dato + botón
private val SizeB = DpSize(140.dp, 110.dp)  // 2×2: columna apilada, sin anillo
private val SizeC = DpSize(250.dp, 48.dp)   // 4×1: banda de dos filas, sin anillo
private val SizeD = DpSize(250.dp, 110.dp)  // 4×2: el único con anillo + CTA ancho

/** El widget siempre usa tema oscuro, así que day y night comparten color. */
private fun cp(color: Color) = ColorProvider(day = color, night = color)

class IteraWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Responsive(setOf(SizeA, SizeB, SizeC, SizeD))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val density = context.resources.displayMetrics.density
        val ringPx = (64 * density).toInt()
        provideContent {
            // Datos REACTIVOS: la composición se mantiene viva y recompone sola
            // cada vez que Room emite un cambio (agua, sesiones, peso, acento).
            // Es clave porque updateAll no reejecuta el código previo a provideContent.
            val dataFlow = remember { loadWidgetDataFlow(context) }
            val data by dataFlow.collectAsState(initial = WidgetData())
            val ring = remember(data.hydrationPercent, data.accent) {
                ringBitmap(data.hydrationPercent, ringPx, data.accent.color)
            }
            WidgetContent(data, ring)
        }
    }
}

/** Solo el ARCO del anillo, dibujado en bitmap porque Glance no ofrece un
 *  progreso circular determinado. El porcentaje NO va aquí: se pinta como texto
 *  nativo de Glance encima ([HydrationRing]), porque algunos launchers (MIUI)
 *  cachean el ImageView del RemoteViews y no repintan el bitmap; el texto nativo
 *  sí se actualiza siempre. */
private fun ringBitmap(percent: Int, sizePx: Int, accent: Color): Bitmap {
    val bmp = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)
    val stroke = sizePx * 0.11f
    val pad = stroke / 2f + sizePx * 0.03f
    val rect = RectF(pad, pad, sizePx - pad, sizePx - pad)

    val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = stroke
        color = RingTrack.toArgb()
    }
    canvas.drawArc(rect, 0f, 360f, false, trackPaint)

    val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = stroke
        strokeCap = Paint.Cap.ROUND
        color = accent.toArgb()
    }
    val sweep = 360f * (percent.coerceIn(0, 100) / 100f)
    canvas.drawArc(rect, -90f, sweep, false, progressPaint)
    return bmp
}

/** Anillo de hidratación: arco (bitmap) + porcentaje en texto NATIVO de Glance
 *  superpuesto, para que el número se actualice siempre aunque el launcher
 *  cachee el bitmap. Al tocarlo abre la vista de hidratación. */
@Composable
private fun HydrationRing(ring: Bitmap, percent: Int, sizeDp: Int, textSp: Int) {
    Box(
        modifier = GlanceModifier
            .size(sizeDp.dp)
            .clickable(actionStartActivity(openAppIntent(LocalContext.current, MainActivity.DEST_HYDRATION))),
        contentAlignment = Alignment.Center
    ) {
        Image(
            provider = ImageProvider(ring),
            contentDescription = "Hidratación $percent%",
            modifier = GlanceModifier.fillMaxSize()
        )
        // La fuente baja según el nº de dígitos, respecto a la base de cada layout:
        // 1-2 dígitos no se encoge; 3 dígitos ("100%") baja 2sp; 4 dígitos ("1000%"), 4sp.
        val percentSp = when {
            percent >= 1000 -> textSp - 4
            percent >= 100 -> textSp - 2
            else -> textSp
        }
        Text(
            "$percent%",
            style = TextStyle(color = cp(TextPrimary), fontSize = percentSp.sp, fontWeight = FontWeight.Bold)
        )
    }
}

@Composable
private fun WidgetContent(data: WidgetData, ring: Bitmap) {
    val size = LocalSize.current
    val narrow = size.width < 200.dp
    val short = size.height < 90.dp
    when {
        narrow && short -> LayoutA(data)
        narrow -> LayoutB(data)
        short -> LayoutC(data)
        else -> LayoutD(data, ring)
    }
}

/** Mancuerna (el mismo icono de la pestaña de entrenamiento): el widget dice
 *  "entrenar", no "reproducir". Tinte según dónde se pinte (sobre acento o neutro). */
@Composable
private fun BarbellIcon(sizeDp: Int, tint: Color) {
    Image(
        provider = ImageProvider(R.drawable.ic_barbell),
        contentDescription = null,
        colorFilter = ColorFilter.tint(cp(tint)),
        modifier = GlanceModifier.size(sizeDp.dp)
    )
}

// OJO (límite de Glance): un Row/Column de Glance admite MÁXIMO 10 hijos; los
// sobrantes se descartan en silencio. Por eso estas filas NO intercalan Spacers
// (7 cajas + 6 spacers = 13 hijos → solo se pintaban 5 días). El hueco entre
// días se hace con padding DENTRO de la celda con peso: 7 hijos exactos, semana
// siempre completa.

/** Barra de progreso semanal como en Home: 7 SEGMENTOS (uno por día L..D).
 *  Entrenado = acento; resto = superficie elevada. */
@Composable
private fun SegmentsBar(trainedDays: Set<Int>, accent: Color, heightDp: Int, gapDp: Int) {
    Row(modifier = GlanceModifier.fillMaxWidth()) {
        (0..6).forEach { i ->
            Box(
                modifier = GlanceModifier
                    .defaultWeight()
                    .padding(end = if (i < 6) gapDp.dp else 0.dp)
            ) {
                Box(
                    modifier = GlanceModifier
                        .fillMaxWidth()
                        .height(heightDp.dp)
                        .cornerRadius((heightDp / 2).dp)
                        .background(if (i in trainedDays) accent else SegmentEmpty),
                    content = {}
                )
            }
        }
    }
}

/** Iniciales L M M J V S D alineadas con los segmentos; el día actual en acento. */
@Composable
private fun InitialsRow(todayIndex: Int, accent: Color, textSp: Int, gapDp: Int) {
    Row(modifier = GlanceModifier.fillMaxWidth()) {
        (0..6).forEach { i ->
            Box(
                modifier = GlanceModifier
                    .defaultWeight()
                    .padding(end = if (i < 6) gapDp.dp else 0.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    DAY_INITIALS[i],
                    style = TextStyle(
                        color = cp(if (i == todayIndex) accent else TextSecondary),
                        fontSize = textSp.sp,
                        fontWeight = if (i == todayIndex) FontWeight.Bold else FontWeight.Medium
                    )
                )
            }
        }
    }
}

/** Botón de entrenar: cuadrado redondeado en acento con la mancuerna. Abre Home. */
@Composable
private fun TrainButton(accent: AccentColor, sizeDp: Int, cornerDp: Int, iconDp: Int) {
    Box(
        modifier = GlanceModifier
            .size(sizeDp.dp)
            .cornerRadius(cornerDp.dp)
            .background(accent.color)
            .clickable(actionStartActivity(openAppIntent(LocalContext.current))),
        contentAlignment = Alignment.Center
    ) {
        BarbellIcon(sizeDp = iconDp, tint = accent.onAccent)
    }
}

/** Intent para abrir la app. Cada destino usa una [Intent.setAction] distinta
 *  para que Glance genere PendingIntents diferenciados (los extras no cuentan
 *  para su deduplicación). */
private fun openAppIntent(context: Context, destination: String? = null): Intent =
    Intent(context, MainActivity::class.java).apply {
        action = "com.luis.itera.OPEN_" + (destination ?: "APP").uppercase()
        if (destination != null) putExtra(MainActivity.EXTRA_DESTINATION, destination)
    }

/** A · 2×1: un dato + acción. Sin marca, sin segmentos, sin iniciales, sin
 *  anillo (no caben). Nada se trunca: lo que no cabe entero, se omite. */
@Composable
private fun LayoutA(data: WidgetData) {
    Row(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(CardBg)
            .cornerRadius(18.dp)
            .clickable(actionStartActivity(openAppIntent(LocalContext.current)))
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = GlanceModifier.defaultWeight()) {
            Text(
                "${data.sessionsThisWeek} / ${data.weeklyGoal}",
                style = TextStyle(color = cp(TextPrimary), fontSize = 18.sp, fontWeight = FontWeight.Bold),
                maxLines = 1
            )
            if (data.streakWeeks > 0) {
                Spacer(GlanceModifier.height(2.dp))
                Text(
                    "racha ${data.streakWeeks} sem",
                    style = TextStyle(color = cp(TextSecondary), fontSize = 11.sp),
                    maxLines = 1
                )
            }
        }
        Spacer(GlanceModifier.width(8.dp))
        TrainButton(data.accent, sizeDp = 42, cornerDp = 12, iconDp = 26)
    }
}

/** B · 2×2: columna, todo apilado. Sin anillo (a este ancho comprimiría la
 *  semana). Botón "Iniciar" a lo ancho. */
@Composable
private fun LayoutB(data: WidgetData) {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(CardBg)
            .cornerRadius(20.dp)
            .clickable(actionStartActivity(openAppIntent(LocalContext.current)))
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Text(
            "ITERA",
            style = TextStyle(color = cp(TitleColor), fontSize = 10.sp, fontWeight = FontWeight.Medium)
        )
        Spacer(GlanceModifier.height(2.dp))
        Text(
            "${data.sessionsThisWeek} / ${data.weeklyGoal}",
            style = TextStyle(color = cp(TextPrimary), fontSize = 24.sp, fontWeight = FontWeight.Bold),
            maxLines = 1
        )
        if (data.streakWeeks > 0) {
            Text(
                "racha ${data.streakWeeks} sem",
                style = TextStyle(color = cp(TextSecondary), fontSize = 11.sp),
                maxLines = 1
            )
        }
        Spacer(GlanceModifier.defaultWeight())
        SegmentsBar(data.trainedDaysThisWeek, data.accent.color, heightDp = 6, gapDp = 3)
        Spacer(GlanceModifier.height(3.dp))
        InitialsRow(data.todayIndex, data.accent.color, textSp = 9, gapDp = 3)
        Spacer(GlanceModifier.defaultWeight())
        Row(
            modifier = GlanceModifier
                .fillMaxWidth()
                .height(38.dp)
                .cornerRadius(12.dp)
                .background(data.accent.color)
                .clickable(actionStartActivity(openAppIntent(LocalContext.current))),
            verticalAlignment = Alignment.CenterVertically,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            BarbellIcon(sizeDp = 20, tint = data.accent.onAccent)
            Spacer(GlanceModifier.width(6.dp))
            Text(
                "Iniciar",
                style = TextStyle(color = cp(data.accent.onAccent), fontSize = 13.sp, fontWeight = FontWeight.Bold)
            )
        }
    }
}

/** C · 4×1: dos filas. SIN anillo (era lo que comprimía la semana): la fila
 *  inferior lleva los 7 segmentos COMPLETOS con sus iniciales. */
@Composable
private fun LayoutC(data: WidgetData) {
    val streakSuffix = if (data.streakWeeks > 0) " · racha ${data.streakWeeks} sem" else ""
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(CardBg)
            .cornerRadius(20.dp)
            .clickable(actionStartActivity(openAppIntent(LocalContext.current)))
            .padding(horizontal = 14.dp, vertical = 9.dp)
    ) {
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = GlanceModifier.defaultWeight()) {
                Text(
                    "ITERA",
                    style = TextStyle(color = cp(TitleColor), fontSize = 10.sp, fontWeight = FontWeight.Medium)
                )
                Spacer(GlanceModifier.height(2.dp))
                Text(
                    "Meta ${data.sessionsThisWeek}/${data.weeklyGoal}$streakSuffix",
                    style = TextStyle(color = cp(TextPrimary), fontSize = 13.sp, fontWeight = FontWeight.Bold),
                    maxLines = 1
                )
            }
            Spacer(GlanceModifier.width(8.dp))
            TrainButton(data.accent, sizeDp = 38, cornerDp = 11, iconDp = 23)
        }
        Spacer(GlanceModifier.defaultWeight())
        SegmentsBar(data.trainedDaysThisWeek, data.accent.color, heightDp = 5, gapDp = 3)
        Spacer(GlanceModifier.height(3.dp))
        InitialsRow(data.todayIndex, data.accent.color, textSp = 10, gapDp = 3)
    }
}

/** D · 4×2: el ÚNICO con anillo. Jerarquía mayor y objetivo táctil grande a lo
 *  ancho. No añade datos que los otros no tengan salvo el anillo. */
@Composable
private fun LayoutD(data: WidgetData, ring: Bitmap) {
    val streakSuffix = if (data.streakWeeks > 0) " · racha ${data.streakWeeks} sem" else ""
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(CardBg)
            .cornerRadius(22.dp)
            .padding(16.dp)
    ) {
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = GlanceModifier.defaultWeight()) {
                Text(
                    "ITERA",
                    style = TextStyle(color = cp(TitleColor), fontSize = 11.sp, fontWeight = FontWeight.Medium)
                )
                Spacer(GlanceModifier.height(4.dp))
                Text(
                    "Meta ${data.sessionsThisWeek}/${data.weeklyGoal}$streakSuffix",
                    style = TextStyle(color = cp(TextPrimary), fontSize = 20.sp, fontWeight = FontWeight.Bold),
                    maxLines = 1
                )
            }
            Spacer(GlanceModifier.width(10.dp))
            HydrationRing(ring, data.hydrationPercent, sizeDp = 52, textSp = 14)
        }

        Spacer(GlanceModifier.defaultWeight())
        SegmentsBar(data.trainedDaysThisWeek, data.accent.color, heightDp = 8, gapDp = 4)
        Spacer(GlanceModifier.height(4.dp))
        InitialsRow(data.todayIndex, data.accent.color, textSp = 11, gapDp = 4)
        Spacer(GlanceModifier.defaultWeight())

        Row(
            modifier = GlanceModifier
                .fillMaxWidth()
                .height(44.dp)
                .cornerRadius(14.dp)
                .background(data.accent.color)
                .clickable(actionStartActivity(openAppIntent(LocalContext.current))),
            verticalAlignment = Alignment.CenterVertically,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            BarbellIcon(sizeDp = 22, tint = data.accent.onAccent)
            Spacer(GlanceModifier.width(8.dp))
            Text(
                "Iniciar entrenamiento",
                style = TextStyle(color = cp(data.accent.onAccent), fontSize = 14.sp, fontWeight = FontWeight.Bold)
            )
        }
    }
}
