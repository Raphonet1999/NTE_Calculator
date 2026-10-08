package com.nte.calculator.data.models

data class StatRecommendation(
    val recommendedStat: StatType,
    val marginalGainPercent: Double,
    val reason: String
)