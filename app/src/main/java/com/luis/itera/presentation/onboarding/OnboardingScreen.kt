package com.luis.itera.presentation.onboarding

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.luis.itera.R
import com.luis.itera.presentation.components.rememberReduceMotion
import com.luis.itera.presentation.theme.IteraColors
import com.luis.itera.presentation.theme.LocalAccent

private data class OnboardingPage(val iconRes: Int, val title: String, val subtitle: String)

private val onboardingPages = listOf(
    OnboardingPage(
        iconRes = R.drawable.ic_barbell,
        title = "Itera",
        subtitle = "Registra tu entrenamiento sin fricción — sin cuentas, sin conexión."
    ),
    OnboardingPage(
        iconRes = R.drawable.ic_flash,
        title = "Anota tus series en segundos",
        subtitle = "Itera recuerda tu última sesión y te sugiere la siguiente progresión."
    ),
    OnboardingPage(
        iconRes = R.drawable.ic_calendar,
        title = "Mira tu progreso",
        subtitle = "Tu constancia, tus récords y tu evolución, de un vistazo."
    )
)

@Composable
fun OnboardingScreen(
    onComplete: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    val pagerState = rememberPagerState(pageCount = { onboardingPages.size })
    val isLastPage = pagerState.currentPage == onboardingPages.lastIndex

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 24.dp)
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            Text(
                "SALTAR",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Medium),
                color = IteraColors.TextSecondary,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { viewModel.onFinish(onComplete) }
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            )
        }

        HorizontalPager(state = pagerState, modifier = Modifier.weight(1f)) { page ->
            OnboardingPageContent(onboardingPages[page])
        }

        PageIndicator(pageCount = onboardingPages.size, currentPage = pagerState.currentPage)
        Spacer(Modifier.height(24.dp))

        if (isLastPage) {
            Button(
                onClick = { viewModel.onFinish(onComplete) },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = LocalAccent.current.color,
                    contentColor = LocalAccent.current.onAccent
                ),
                shape = RoundedCornerShape(8.dp)
            ) { Text("EMPEZAR", style = MaterialTheme.typography.titleMedium) }
        } else {
            Spacer(Modifier.height(48.dp))
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun OnboardingPageContent(page: OnboardingPage) {
    val reduceMotion = rememberReduceMotion()
    var visible by remember(page) { mutableStateOf(false) }
    LaunchedEffect(page) { visible = true }

    val entryDuration = if (reduceMotion) 0 else 260
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(entryDuration, easing = FastOutSlowInEasing),
        label = "onboarding_alpha"
    )
    val offsetY by animateFloatAsState(
        targetValue = if (!reduceMotion && !visible) 16f else 0f,
        animationSpec = tween(entryDuration, easing = FastOutSlowInEasing),
        label = "onboarding_offset"
    )

    Column(
        Modifier
            .fillMaxSize()
            .padding(top = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier
                .size(112.dp)
                .graphicsAlphaOffset(alpha, offsetY)
                .clip(RoundedCornerShape(28.dp))
                .background(IteraColors.SurfaceElevated),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                ImageVector.vectorResource(page.iconRes),
                contentDescription = null,
                tint = LocalAccent.current.color,
                modifier = Modifier.size(52.dp)
            )
        }
        Spacer(Modifier.height(40.dp))
        Text(
            page.title,
            style = MaterialTheme.typography.headlineSmall,
            color = IteraColors.TextPrimary,
            textAlign = TextAlign.Center,
            modifier = Modifier.graphicsAlphaOffset(alpha, offsetY)
        )
        Spacer(Modifier.height(12.dp))
        Text(
            page.subtitle,
            style = MaterialTheme.typography.bodyLarge,
            color = IteraColors.TextSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.graphicsAlphaOffset(alpha, offsetY)
        )
    }
}

/** Fade + desplazamiento vertical corto para la entrada de cada elemento (con
 *  reduceMotion, [alpha] y [offsetYDp] ya llegan fijos en 1f/0f: cambio directo). */
private fun Modifier.graphicsAlphaOffset(alpha: Float, offsetYDp: Float): Modifier =
    this
        .alpha(alpha)
        .offset(y = offsetYDp.dp)

@Composable
private fun PageIndicator(pageCount: Int, currentPage: Int) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center
    ) {
        repeat(pageCount) { index ->
            val selected = index == currentPage
            Box(
                Modifier
                    .padding(horizontal = 4.dp)
                    .size(if (selected) 9.dp else 7.dp)
                    .clip(RoundedCornerShape(50))
                    .background(if (selected) LocalAccent.current.color else IteraColors.SurfaceElevated)
            )
        }
    }
}
