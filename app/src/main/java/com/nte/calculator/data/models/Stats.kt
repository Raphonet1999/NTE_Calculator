package com.nte.calculator.data.models

data class Stats(
    val baseAtk: Double = 0.0,
    val atkPercent: Double = 0.0,
    val flatAtk: Double = 0.0,
    val critRatePercent: Double = 0.0,
    val critDmgPercent: Double = 0.0,
    val dmgBonusPercent: Double = 0.0
)

/**
 * Modelo de datos que representa un valor de estadística individual.
 */
data class StatValue(
    val type: StatType,
    val value: Double
)

/**
 * Contenedor para almacenar el conjunto completo de estadísticas de un personaje o equipamiento.
 */
data class CharacterStats(
    val baseAtk: Double = 0.0,
    val atkPercent: Double = 0.0,
    val flatAtk: Double = 0.0,
    val critRatePercent: Double = 0.0,
    val critDmgPercent: Double = 0.0,
    val dmgBonusPercent: Double = 0.0,
    val statsList: List<StatValue> = emptyList()
)
