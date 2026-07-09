package com.luis.itera.presentation.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Convención tipográfica de Itera:
//  - Sans-serif (familia por defecto) como fuente PRINCIPAL de toda la prosa y etiquetas.
//  - Monospace como ACENTO reservado a lecturas numéricas prominentes; vive en `titleLarge`,
//    que en la app solo se usa para números grandes (totales, valores de stats, stepper, etc.).
//  Regla: elegir la familia según el CONTENIDO (dato vs prosa), no según el tamaño del slot.
val IteraTypography = Typography(
    headlineSmall = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 22.sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Normal,
        fontSize = 20.sp
    ),
    titleMedium = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp
    ),
    bodyMedium = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp
    ),
    bodySmall = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp
    ),
    labelSmall = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 10.sp,
        letterSpacing = 1.sp
    )
)