package com.nte.calculator.data.models

enum class StatType(
    val displayName: String,
    val isPercentage: Boolean = false
) {
    BASE_ATK("Ataque Base"),
    ATK_PERCENT("Ataque %", isPercentage = true),
    FLAT_ATK("Ataque base (arma o personaje) de ser posible de lo contrario sube ataque plano"),
    CRIT_RATE("Prob. Crítica %", isPercentage = true),
    CRIT_DMG("Daño Crítico %", isPercentage = true),
    DMG_BONUS("Bono de Daño %", isPercentage = true),
    ELEMENTAL_MASTERY("Maestría Elemental"),
    ENERGY_RECHARGE("Recarga de Energía %", isPercentage = true),
    DEF_PERCENT("Defensa %", isPercentage = true),
    HP_PERCENT("Vida %", isPercentage = true)
}