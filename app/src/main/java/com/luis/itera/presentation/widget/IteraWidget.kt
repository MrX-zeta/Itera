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

private val CardBg = Color(0xFF141418)
private val Accent = Color(0xFF2DD4BF)
private val ButtonFg = Color(0xFF0D0D11)
private val TextPrimary = Color(0xFFE8E8EA)
private val TextSecondary = Color(0xFF8A8A92)
private val TitleColor = Color(0xFF9E9EA6)
private val DotEmpty = Color(0xFF3E3E48)
private val RingTrack = Color(0xFF33333B)

private val SlimSize = DpSize(300.dp, 60.dp)
private val WideSize = DpSize(300.dp, 110.dp)
private val CompactSize = DpSize(140.dp, 140.dp)

/** El widget siempre usa tema oscuro, así que day y night comparten color. */
private fun cp(color: Color) = ColorProvider(day = color, night = color)

class IteraWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Responsive(setOf(SlimSize, WideSize, CompactSize))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val density = context.resources.displayMetrics.density
        val ringPx = (46 * density).toInt()
        provideContent {
            // Datos REACTIVOS: la composición se mantiene viva y recompone sola
            // cada vez que Room emite un cambio (agua, sesiones, peso). Es clave
            // porque updateAll no reejecuta el código previo a provideContent.
            val dataFlow = remember { loadWidgetDataFlow(context) }
            val data by dataFlow.collectAsState(initial = WidgetData())
            val ring = remember(data.hydrationPercent) { ringBitmap(data.hydrationPercent, ringPx) }
            WidgetContent(data, ring)
        }
    }
}

/** Solo el ARCO del anillo, dibujado en bitmap porque Glance no ofrece un
 *  progreso circular determinado. El porcentaje NO va aquí: se pinta como texto
 *  nativo de Glance encima ([HydrationRing]), porque algunos launchers (MIUI)
 *  cachean el ImageView del RemoteViews y no repintan el bitmap; el texto nativo
 *  sí se actualiza siempre. */
private fun ringBitmap(percent: Int, sizePx: Int): Bitmap {
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
        color = Accent.toArgb()
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
        Text(
            "$percent%",
            style = TextStyle(color = cp(TextPrimary), fontSize = textSp.sp, fontWeight = FontWeight.Bold)
        )
    }
}

@Composable
private fun WidgetContent(data: WidgetData, ring: Bitmap) {
    val size = LocalSize.current
    when {
        size.height < 85.dp -> SlimLayout(data, ring)
        size.width < 220.dp -> CompactLayout(data, ring)
        else -> WideLayout(data, ring)
    }
}

@Composable
private fun DotsRow(trainedDays: Set<Int>, dotSize: Int, gap: Int) {
    Row {
        (0..6).forEach { i ->
            Box(
                modifier = GlanceModifier
                    .size(dotSize.dp)
                    .cornerRadius((dotSize / 2).dp)
                    .background(if (i in trainedDays) Accent else DotEmpty),
                content = {}
            )
            if (i < 6) Spacer(GlanceModifier.width(gap.dp))
        }
    }
}

@Composable
private fun PlayIcon(sizeDp: Int, modifier: GlanceModifier = GlanceModifier) {
    Image(
        provider = ImageProvider(R.drawable.ic_widget_play),
        contentDescription = null,
        modifier = modifier.size(sizeDp.dp)
    )
}

/** Intent para abrir la app. Cada destino usa una [Intent.setAction] distinta
 *  para que Glance genere PendingIntents diferenciados (los extras no cuentan
 *  para su deduplicación). */
private fun openAppIntent(context: Context, destination: String? = null): Intent =
    Intent(context, MainActivity::class.java).apply {
        action = "com.luis.itera.OPEN_" + (destination ?: "APP").uppercase()
        if (destination != null) putExtra(MainActivity.EXTRA_DESTINATION, destination)
    }

@Composable
private fun WideLayout(data: WidgetData, ring: Bitmap) {
    val streakSuffix = if (data.streakWeeks > 0) " · RACHA ${data.streakWeeks} SEM" else ""
    Row(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(CardBg)
            .cornerRadius(22.dp)
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = GlanceModifier.defaultWeight()) {
            Text(
                "ITERA",
                style = TextStyle(color = cp(TitleColor), fontSize = 12.sp, fontWeight = FontWeight.Medium)
            )
            Spacer(GlanceModifier.height(6.dp))
            Text(
                "META ${data.sessionsThisWeek}/${data.weeklyGoal}$streakSuffix",
                style = TextStyle(color = cp(TextPrimary), fontSize = 15.sp, fontWeight = FontWeight.Bold)
            )
            Spacer(GlanceModifier.height(10.dp))
            DotsRow(data.trainedDaysThisWeek, dotSize = 13, gap = 6)
        }

        Spacer(GlanceModifier.width(14.dp))

        HydrationRing(ring, data.hydrationPercent, sizeDp = 44, textSp = 13)

        Spacer(GlanceModifier.width(12.dp))

        Box(
            modifier = GlanceModifier
                .size(48.dp)
                .cornerRadius(15.dp)
                .background(Accent)
                .clickable(actionStartActivity(openAppIntent(LocalContext.current))),
            contentAlignment = Alignment.Center
        ) {
            PlayIcon(sizeDp = 22)
        }
    }
}

@Composable
private fun SlimLayout(data: WidgetData, ring: Bitmap) {
    Row(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(CardBg)
            .cornerRadius(18.dp)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Bloque izquierdo con peso: absorbe el espacio sobrante y, si el ancho
        // es muy justo, recorta los puntos antes de empujar fuera el anillo o el
        // botón (que quedan fijos a la derecha y nunca se cortan).
        Row(
            modifier = GlanceModifier.defaultWeight(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "META ${data.sessionsThisWeek}/${data.weeklyGoal}",
                style = TextStyle(color = cp(TextPrimary), fontSize = 14.sp, fontWeight = FontWeight.Bold),
                maxLines = 1
            )
            Spacer(GlanceModifier.width(10.dp))
            DotsRow(data.trainedDaysThisWeek, dotSize = 10, gap = 4)
        }

        Spacer(GlanceModifier.width(10.dp))

        HydrationRing(ring, data.hydrationPercent, sizeDp = 38, textSp = 12)

        Spacer(GlanceModifier.width(8.dp))

        Box(
            modifier = GlanceModifier
                .size(38.dp)
                .cornerRadius(12.dp)
                .background(Accent)
                .clickable(actionStartActivity(openAppIntent(LocalContext.current))),
            contentAlignment = Alignment.Center
        ) {
            PlayIcon(sizeDp = 16)
        }
    }
}

@Composable
private fun CompactLayout(data: WidgetData, ring: Bitmap) {
    val streakSuffix = if (data.streakWeeks > 0) " · ${data.streakWeeks} SEM" else ""
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
            Text(
                "ITERA",
                style = TextStyle(color = cp(TitleColor), fontSize = 12.sp, fontWeight = FontWeight.Medium),
                modifier = GlanceModifier.defaultWeight()
            )
            HydrationRing(ring, data.hydrationPercent, sizeDp = 34, textSp = 11)
        }

        Spacer(GlanceModifier.height(10.dp))
        Text(
            "${data.sessionsThisWeek}/${data.weeklyGoal}$streakSuffix",
            style = TextStyle(color = cp(TextPrimary), fontSize = 15.sp, fontWeight = FontWeight.Bold)
        )
        Spacer(GlanceModifier.height(10.dp))
        DotsRow(data.trainedDaysThisWeek, dotSize = 12, gap = 5)

        Spacer(GlanceModifier.defaultWeight())

        Row(
            modifier = GlanceModifier
                .fillMaxWidth()
                .height(40.dp)
                .cornerRadius(13.dp)
                .background(Accent)
                .clickable(actionStartActivity(openAppIntent(LocalContext.current))),
            verticalAlignment = Alignment.CenterVertically,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Sube el ícono ~1.5dp: el texto en mayúsculas queda ópticamente
            // más arriba que el centro de su caja, así que centrarlos a secas
            // deja el play visualmente por debajo del texto.
            PlayIcon(sizeDp = 16, modifier = GlanceModifier.padding(bottom = 3.dp))
            Spacer(GlanceModifier.width(6.dp))
            Text(
                "INICIAR",
                style = TextStyle(color = cp(ButtonFg), fontSize = 14.sp, fontWeight = FontWeight.Bold)
            )
        }
    }
}
