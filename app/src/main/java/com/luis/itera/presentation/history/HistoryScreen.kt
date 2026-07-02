package com.luis.itera.presentation.history

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kizitonwose.calendar.compose.HorizontalCalendar
import com.kizitonwose.calendar.compose.rememberCalendarState
import com.kizitonwose.calendar.core.CalendarDay
import com.kizitonwose.calendar.core.DayPosition
import com.kizitonwose.calendar.core.firstDayOfWeekFromLocale
import com.luis.itera.R
import com.luis.itera.domain.model.Session
import com.luis.itera.presentation.theme.IteraColors
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

private val monthFormatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale("es"))

@Composable
fun HistoryScreen(
    onSessionClick: (Long) -> Unit,
    viewModel: HistoryViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    val currentMonth = remember { YearMonth.now() }
    val calendarState = rememberCalendarState(
        startMonth = currentMonth.minusMonths(12),
        endMonth = currentMonth,
        firstVisibleMonth = currentMonth,
        firstDayOfWeek = firstDayOfWeekFromLocale()
    )

    Column(
        Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "HISTORIAL",
            style = MaterialTheme.typography.labelSmall,
            color = IteraColors.TextSecondary
        )
        Spacer(Modifier.height(12.dp))

        Text(
            text = calendarState.firstVisibleMonth.yearMonth.format(monthFormatter)
                .replaceFirstChar { it.uppercase() },
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(Modifier.height(8.dp))

        HorizontalCalendar(
            state = calendarState,
            dayContent = { day ->
                DayCell(
                    day = day,
                    isSelected = day.date == state.selectedDate,
                    isTrained = day.date in state.trainedDays,
                    onClick = { viewModel.onDateSelected(day.date) }
                )
            }
        )

        Spacer(Modifier.height(16.dp))

        if (state.sessions.isEmpty()) {
            Text(
                text = "Sin sesiones registradas",
                style = MaterialTheme.typography.bodySmall,
                color = IteraColors.TextSecondary
            )
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(state.sessions, key = { it.id }) { session ->
                    SessionCard(
                        session = session,
                        exerciseNames = state.exerciseNames,
                        onClick = { onSessionClick(session.id) },
                        onDelete = { viewModel.onDeleteSession(session.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun DayCell(
    day: CalendarDay,
    isSelected: Boolean,
    isTrained: Boolean,
    onClick: () -> Unit
) {
    val inMonth = day.position == DayPosition.MonthDate
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .padding(2.dp)
            .then(
                if (isSelected) Modifier.border(1.dp, IteraColors.Accent, CircleShape)
                else Modifier
            )
            .clickable(enabled = inMonth, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = day.date.dayOfMonth.toString(),
                style = MaterialTheme.typography.bodySmall,
                color = when {
                    !inMonth -> IteraColors.Border
                    isSelected -> IteraColors.Accent
                    else -> IteraColors.TextPrimary
                }
            )
            if (isTrained && inMonth) {
                Box(
                    Modifier
                        .size(4.dp)
                        .background(IteraColors.Accent, CircleShape)
                )
            } else {
                Spacer(Modifier.size(4.dp))
            }
        }
    }
}

@Composable
private fun SessionCard(
    session: Session,
    exerciseNames: Map<Long, String>,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Column(
        Modifier
            .fillMaxWidth()
            .border(1.dp, IteraColors.Border, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(12.dp)
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "SESIÓN ${session.id}",
                style = MaterialTheme.typography.labelSmall,
                color = IteraColors.TextSecondary
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (session.isFinished) "${session.durationMinutes} min" else "EN CURSO",
                    style = MaterialTheme.typography.bodySmall,
                    color = IteraColors.Accent
                )
                Icon(
                    imageVector = ImageVector.vectorResource(R.drawable.ic_trash),
                    contentDescription = "Eliminar sesión",
                    tint = IteraColors.TextSecondary,
                    modifier = Modifier
                        .padding(start = 10.dp)
                        .clickable(onClick = onDelete)
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        session.sets
            .groupBy { it.exerciseId }
            .forEach { (exerciseId, sets) ->
                Column(Modifier.padding(vertical = 4.dp)) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = exerciseNames[exerciseId] ?: "—",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "${sets.size} sets",
                            style = MaterialTheme.typography.bodySmall,
                            color = IteraColors.Accent
                        )
                    }
                    Text(
                        text = sets.joinToString(" · ") { set ->
                            set.reps.toString() +
                                    if (set.weightAddedKg > 0f) "+${set.weightAddedKg}kg" else ""
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = IteraColors.TextSecondary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
    }
}