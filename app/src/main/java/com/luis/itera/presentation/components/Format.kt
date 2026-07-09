package com.luis.itera.presentation.components

import java.util.Locale

enum class VolumeUnit { KG, TON }

fun volumeUnitFor(maxValue: Float): VolumeUnit =
    if (maxValue >= 10_000f) VolumeUnit.TON else VolumeUnit.KG

fun formatVolume(value: Float, unit: VolumeUnit): String = when (unit) {
    VolumeUnit.TON -> String.format(Locale.US, "%.1f ton", value / 1000f)
    VolumeUnit.KG -> String.format(Locale.US, "%,d kg", value.toInt())
}

fun fmtWeight(value: Float): String =
    if (value % 1f == 0f) value.toInt().toString() else String.format(Locale.US, "%.1f", value)
