package com.luis.itera.presentation.settings

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.luis.itera.R
import com.luis.itera.presentation.components.FastStepper
import com.luis.itera.presentation.theme.AccentColor
import com.luis.itera.presentation.theme.IteraColors
import com.luis.itera.presentation.theme.LocalAccent

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.widgetMessage.collect { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    Column(
        Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    ImageVector.vectorResource(R.drawable.ic_back),
                    contentDescription = "Volver",
                    tint = IteraColors.TextPrimary
                )
            }
            Text("Ajustes", style = MaterialTheme.typography.headlineSmall, color = IteraColors.TextPrimary)
        }

        Column(
            Modifier.verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            SettingsSection("APARIENCIA") {
                Text(
                    "Color de acento",
                    style = MaterialTheme.typography.titleMedium,
                    color = IteraColors.TextPrimary
                )
                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                    AccentSwatch(
                        accent = AccentColor.TEAL,
                        label = "Teal",
                        selected = state.accentColor == AccentColor.TEAL,
                        onClick = { viewModel.onAccentSelected(AccentColor.TEAL) }
                    )
                    AccentSwatch(
                        accent = AccentColor.INDIGO,
                        label = "Índigo",
                        selected = state.accentColor == AccentColor.INDIGO,
                        onClick = { viewModel.onAccentSelected(AccentColor.INDIGO) }
                    )
                    AccentSwatch(
                        accent = AccentColor.LIME,
                        label = "Lima",
                        selected = state.accentColor == AccentColor.LIME,
                        onClick = { viewModel.onAccentSelected(AccentColor.LIME) }
                    )
                }
            }

            SettingsSection("WIDGET") {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(onClick = viewModel::onAddWidgetClick)
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            "Añadir a la pantalla de inicio",
                            style = MaterialTheme.typography.titleMedium,
                            color = IteraColors.TextPrimary
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Anclar el widget de Itera fuera de la app",
                            style = MaterialTheme.typography.bodyMedium,
                            color = IteraColors.TextSecondary
                        )
                    }
                    Icon(
                        ImageVector.vectorResource(R.drawable.ic_chevron_right),
                        contentDescription = null,
                        tint = IteraColors.TextSecondary
                    )
                }
            }

            SettingsSection("PERFIL") {
                Text(
                    "Peso corporal",
                    style = MaterialTheme.typography.titleMedium,
                    color = IteraColors.TextPrimary
                )
                Spacer(Modifier.height(10.dp))
                FastStepper(
                    label = "PESO CORPORAL (KG)",
                    value = state.weightKg,
                    onDelta = viewModel::onWeightDelta
                )

                Spacer(Modifier.height(20.dp))
                Text(
                    "Meta semanal",
                    style = MaterialTheme.typography.titleMedium,
                    color = IteraColors.TextPrimary
                )
                Spacer(Modifier.height(10.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    (1..7).forEach { day ->
                        val selected = day == state.weeklyGoal
                        Box(
                            Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (selected) LocalAccent.current.color else IteraColors.Surface)
                                .clickable { viewModel.onGoalSelected(day) }
                                .padding(vertical = 14.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "$day",
                                style = MaterialTheme.typography.titleMedium,
                                color = if (selected) LocalAccent.current.onAccent else IteraColors.TextPrimary
                            )
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Acerca de Itera",
                        style = MaterialTheme.typography.titleMedium,
                        color = IteraColors.TextPrimary
                    )
                    Text(
                        if (state.appVersion.isNotBlank()) "Versión ${state.appVersion}" else "",
                        style = MaterialTheme.typography.bodyMedium,
                        color = IteraColors.TextSecondary
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable () -> Unit) {
    Column {
        Text(
            title,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
            color = IteraColors.TextTertiary
        )
        Spacer(Modifier.height(10.dp))
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(IteraColors.SurfaceElevated)
                .padding(16.dp)
        ) {
            content()
        }
    }
}

@Composable
private fun AccentSwatch(
    accent: AccentColor,
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Box(
            Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(accent.color)
                .then(
                    if (selected) {
                        Modifier.border(2.dp, IteraColors.TextPrimary, RoundedCornerShape(12.dp))
                    } else {
                        Modifier
                    }
                )
        )
        Spacer(Modifier.height(8.dp))
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = if (selected) IteraColors.TextPrimary else IteraColors.TextSecondary
        )
    }
}
