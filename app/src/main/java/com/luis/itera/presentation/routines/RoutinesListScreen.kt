package com.luis.itera.presentation.routines

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import com.luis.itera.R
import com.luis.itera.domain.model.Routine
import com.luis.itera.presentation.active_workout.RoutineCard
import com.luis.itera.presentation.theme.IteraColors

/**
 * Lista COMPLETA de rutinas, mínima: reutiliza los datos que ya expone
 * ActiveWorkoutViewModel (misma instancia, compartida vía back stack entry) para
 * no duplicar la lógica de onStartRoutine. Se sustituirá por la pantalla de
 * Rutinas definitiva (con creación/edición) en un prompt posterior.
 */
@Composable
fun RoutinesListScreen(
    routines: List<Routine>,
    onBack: () -> Unit,
    onStartRoutine: (Routine) -> Unit
) {
    Column(
        Modifier
            .fillMaxSize()
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
            Text("Tus rutinas", style = MaterialTheme.typography.headlineSmall, color = IteraColors.TextPrimary)
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(routines, key = { it.id }) { routine ->
                RoutineCard(
                    routine = routine,
                    onClick = { onStartRoutine(routine) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
