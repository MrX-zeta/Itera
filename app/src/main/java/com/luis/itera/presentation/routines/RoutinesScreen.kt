package com.luis.itera.presentation.routines

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.luis.itera.R
import com.luis.itera.presentation.theme.IteraColors
import com.luis.itera.presentation.theme.LocalAccent
import com.luis.itera.presentation.theme.RoutineColor

@Composable
fun RoutinesScreen(
    onCreate: () -> Unit,
    onEdit: (Long) -> Unit,
    onStart: () -> Unit,
    viewModel: RoutinesViewModel = hiltViewModel()
) {
    val routines by viewModel.routines.collectAsStateWithLifecycle()

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 16.dp)
            .padding(top = 20.dp)
    ) {
        Text(
            "RUTINAS",
            style = MaterialTheme.typography.titleMedium,
            color = IteraColors.TextSecondary,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(16.dp))

        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(LocalAccent.current.color)
                .clickable(onClick = onCreate)
                .padding(vertical = 14.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                ImageVector.vectorResource(R.drawable.ic_plus),
                contentDescription = null,
                tint = LocalAccent.current.onAccent,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                "Nueva rutina",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = LocalAccent.current.onAccent
            )
        }
        Spacer(Modifier.height(16.dp))

        if (routines.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(bottom = 48.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "Aún no tienes rutinas",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = IteraColors.TextPrimary
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Crea una para empezar tus entrenamientos más rápido.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = IteraColors.TextSecondary,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(routines, key = { it.id }) { routine ->
                    RoutineManageCard(
                        routine = routine,
                        onEdit = { onEdit(routine.id) },
                        onStart = {
                            viewModel.requestStart(routine.id)
                            onStart()
                        }
                    )
                }
            }
        }
    }
}

/** Card de la pestaña Rutinas: el cuerpo abre el editor; el botón de play inicia la rutina. */
@Composable
private fun RoutineManageCard(routine: com.luis.itera.domain.model.Routine, onEdit: () -> Unit, onStart: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(IteraColors.SurfaceElevated)
            .height(IntrinsicSize.Min),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            Modifier
                .weight(1f)
                .fillMaxHeight()
                .clickable(onClick = onEdit)
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(2.dp))
                    .background(RoutineColor.fromOrdinal(routine.color).color)
            )
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    routine.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = IteraColors.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    "${routine.exerciseIds.size} ejercicios",
                    style = MaterialTheme.typography.bodyMedium,
                    color = IteraColors.TextSecondary
                )
            }
        }
        Box(
            Modifier
                .fillMaxHeight()
                .clickable(onClick = onStart),
            contentAlignment = Alignment.Center
        ) {
            Box(
                Modifier
                    .padding(12.dp)
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(LocalAccent.current.color),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    ImageVector.vectorResource(R.drawable.ic_widget_play),
                    contentDescription = "Iniciar rutina",
                    tint = LocalAccent.current.onAccent,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
