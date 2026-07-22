package com.luis.itera.presentation.settings

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Widgets
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
        Box(
            Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            IconButton(onClick = onBack, modifier = Modifier.align(Alignment.CenterStart)) {
                Icon(
                    ImageVector.vectorResource(R.drawable.ic_back),
                    contentDescription = "Volver",
                    tint = IteraColors.TextSecondary
                )
            }
            Text(
                "AJUSTES",
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.5.sp
                ),
                color = IteraColors.TextPrimary
            )
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
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
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

            SettingsSection("WIDGET", icon = Icons.Rounded.Widgets) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(onClick = viewModel::onAddWidgetClick)
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Rounded.Widgets,
                            contentDescription = null,
                            tint = IteraColors.TextSecondary,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                "Añadir a pantalla de inicio",
                                style = MaterialTheme.typography.titleMedium,
                                color = IteraColors.TextPrimary
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "Coloca el widget de Itera",
                                style = MaterialTheme.typography.bodyMedium,
                                color = IteraColors.TextSecondary
                            )
                        }
                    }
                    Icon(
                        ImageVector.vectorResource(R.drawable.ic_chevron_right),
                        contentDescription = null,
                        tint = IteraColors.TextSecondary
                    )
                }
            }

            Column {
                SectionLabel("PERFIL")
                Spacer(Modifier.height(10.dp))
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SettingsCard {
                        FastStepper(
                            label = "PESO CORPORAL (KG)",
                            value = state.weightKg,
                            onDelta = viewModel::onWeightDelta
                        )
                    }

                    SettingsCard {
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
                    }

                    SettingsCard {
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
                                if (state.appVersion.isNotBlank()) "v${state.appVersion}" else "",
                                style = MaterialTheme.typography.bodyMedium,
                                color = IteraColors.TextSecondary
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun SectionLabel(title: String, icon: ImageVector? = null) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (icon != null) {
            Icon(
                icon,
                contentDescription = null,
                tint = IteraColors.TextSecondary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(6.dp))
        }
        Text(
            title,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium, letterSpacing = 0.5.sp),
            color = IteraColors.TextTertiary
        )
    }
}

@Composable
private fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
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

@Composable
private fun SettingsSection(title: String, icon: ImageVector? = null, content: @Composable ColumnScope.() -> Unit) {
    Column {
        SectionLabel(title, icon)
        Spacer(Modifier.height(10.dp))
        SettingsCard(content = content)
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
                .size(64.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(accent.color)
                .then(
                    if (selected) {
                        Modifier.border(2.dp, IteraColors.TextPrimary, RoundedCornerShape(18.dp))
                    } else {
                        Modifier
                    }
                )
        )
        Spacer(Modifier.height(8.dp))
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = if (selected) IteraColors.TextPrimary else IteraColors.TextSecondary,
            textAlign = TextAlign.Center
        )
    }
}
