package com.luis.itera.presentation.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.luis.itera.presentation.components.FastStepper
import com.luis.itera.presentation.theme.IteraColors
import com.luis.itera.presentation.theme.LocalAccent

@Composable
fun OnboardingScreen(
    onComplete: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(24.dp)
    ) {
        Spacer(Modifier.height(48.dp))
        AnimatedContent(
            targetState = state.step,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "onboarding_step",
            modifier = Modifier.weight(1f)
        ) { step ->
            when (step) {
                0 -> WeightStep(state.weightKg, viewModel::onWeightDelta)
                else -> GoalStep(state.weeklyGoal, viewModel::onGoalSelected)
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (state.step == 1) {
                Button(
                    onClick = viewModel::onBack,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = IteraColors.Surface, contentColor = IteraColors.TextPrimary),
                    shape = RoundedCornerShape(8.dp)
                ) { Text("ATRÁS", style = MaterialTheme.typography.titleMedium) }
            }
            Button(
                onClick = { if (state.step == 0) viewModel.onNext() else viewModel.onFinish(onComplete) },
                modifier = Modifier.weight(if (state.step == 1) 2f else 1f),
                colors = ButtonDefaults.buttonColors(containerColor = LocalAccent.current.color, contentColor = LocalAccent.current.onAccent),
                shape = RoundedCornerShape(8.dp)
            ) { Text(if (state.step == 0) "CONTINUAR" else "EMPEZAR", style = MaterialTheme.typography.titleMedium) }
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun WeightStep(weightKg: Float, onDelta: (Float) -> Unit) {
    Column(verticalArrangement = Arrangement.Center) {
        Text("¿Cuánto pesas?", style = MaterialTheme.typography.headlineSmall, color = IteraColors.TextPrimary)
        Spacer(Modifier.height(8.dp))
        Text("Calcula tu meta de hidratación diaria", style = MaterialTheme.typography.bodyMedium, color = IteraColors.TextSecondary)
        Spacer(Modifier.height(32.dp))
        FastStepper(label = "PESO CORPORAL (KG)", value = weightKg, onDelta = onDelta)
    }
}

@Composable
private fun GoalStep(weeklyGoal: Int, onSelect: (Int) -> Unit) {
    Column(verticalArrangement = Arrangement.Center) {
        Text("¿Cuántos días por semana?", style = MaterialTheme.typography.headlineSmall, color = IteraColors.TextPrimary)
        Spacer(Modifier.height(8.dp))
        Text("Define tu meta semanal y tu racha", style = MaterialTheme.typography.bodyMedium, color = IteraColors.TextSecondary)
        Spacer(Modifier.height(32.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(2, 3, 4, 5, 6).forEach { day ->
                val selected = day == weeklyGoal
                Box(
                    Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (selected) LocalAccent.current.color else IteraColors.Surface)
                        .border(1.dp, if (selected) LocalAccent.current.color else IteraColors.BorderStrong, RoundedCornerShape(10.dp))
                        .clickable { onSelect(day) }
                        .padding(vertical = 20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("$day", style = MaterialTheme.typography.titleLarge.copy(fontSize = 22.sp, fontWeight = FontWeight.Bold), color = if (selected) LocalAccent.current.onAccent else IteraColors.TextPrimary)
                }
            }
        }
    }
}