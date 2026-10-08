package com.nte.calculator.domain

import com.nte.calculator.data.models.CharacterStats
import com.nte.calculator.data.models.StatRecommendation
import com.nte.calculator.data.models.StatType
import com.nte.calculator.data.models.Stats
import kotlin.math.min

object CalculatorEngine {

    // =========================================================================
    // 1. STATS INNATOS BASE DEL PERSONAJE (Sin equipamiento)
    // =========================================================================
    const val INNATE_CRIT_RATE = 5.0    // 5% Probabilidad Crítica Base
    const val INNATE_CRIT_DMG = 50.0   // 50% Daño Crítico Base

    // =========================================================================
    // 2. VALORES UNITARIOS DE MEJORA POR 1 CUADRO DE MÓDULO (Unidad de Coste)
    // =========================================================================
    private const val MODULE_SLOT_ATK_PERCENT = 1.25     // +1.25% ATQ%
    private const val MODULE_SLOT_FLAT_ATK = 8.0        // +8 ATQ Plano
    private const val MODULE_SLOT_CRIT_RATE = 1.0       // +1.0% Prob. Crítica
    private const val MODULE_SLOT_CRIT_DMG = 2.0        // +2.0% Daño Crítico
    private const val MODULE_SLOT_UNIVERSAL_DMG = 1.0   // +1.0% Bono Universal

    // LÍMITES MÁXIMOS EN MÓDULOS (20 Cuadros Totales)
    const val MAX_MODULE_SLOTS = 20
    const val MAX_MODULE_ATK_PERCENT = 25.0      // 20 cuadros * 1.25%
    const val MAX_MODULE_FLAT_ATK = 160.0        // 20 cuadros * 8
    const val MAX_MODULE_CRIT_RATE = 20.0       // 20 cuadros * 1.0%
    const val MAX_MODULE_CRIT_DMG = 40.0        // 20 cuadros * 2.0%
    const val MAX_MODULE_UNIVERSAL_DMG = 20.0   // 20 cuadros * 1.0%

    // =========================================================================
    // 3. MÁXIMOS DEL CARTUCHO
    // =========================================================================
    // Stat Principal (Solo 1 opción de las siguientes):
    const val CARTRIDGE_MAIN_ATK_PERCENT = 37.5
    const val CARTRIDGE_MAIN_CRIT_RATE = 30.0
    const val CARTRIDGE_MAIN_CRIT_DMG = 60.0
    const val CARTRIDGE_MAIN_ELEMENTAL_DMG = 37.5

    // Substats del Cartucho (Hasta 4 opciones elegidas):
    const val CARTRIDGE_SUB_FLAT_ATK = 80.0
    const val CARTRIDGE_SUB_ATK_PERCENT = 12.5
    const val CARTRIDGE_SUB_CRIT_RATE = 10.0
    const val CARTRIDGE_SUB_CRIT_DMG = 20.0
    const val CARTRIDGE_SUB_UNIVERSAL_DMG = 10.0

    /**
     * Calcula el Daño Base Esperado asegurando los 5% CR y 50% CD innatos.
     */
    fun calculateExpectedDamage(stats: Stats, skillMultiplierPercent: Double = 100.0): Double {
        val totalAtk = stats.baseAtk * (1 + stats.atkPercent / 100.0) + stats.flatAtk
        val skillMultiplier = skillMultiplierPercent / 100.0

        // Garantizar al menos los stats innatos del personaje (5% CR / 50% CD)
        val finalCritRate = stats.critRatePercent.coerceAtLeast(INNATE_CRIT_RATE)
        val finalCritDmg = stats.critDmgPercent.coerceAtLeast(INNATE_CRIT_DMG)

        // Regla de Oro: La Probabilidad Crítica no aporta daño por encima del 100%
        val effectiveCritRate = min(finalCritRate, 100.0) / 100.0
        val critFactor = 1 + (effectiveCritRate * (finalCritDmg / 100.0))
        val dmgBonusFactor = 1 + (stats.dmgBonusPercent / 100.0)

        return totalAtk * skillMultiplier * critFactor * dmgBonusFactor
    }

    fun calculateDamage(stats: CharacterStats, skillMultiplierPercent: Double = 100.0): Double {
        return calculateExpectedDamage(stats.toStats(), skillMultiplierPercent)
    }

    /**
     * Recomienda qué subir en el SIGUIENTE cuadro de módulo (o substat de cartucho)
     * calculando el verdadero coste de oportunidad respecto al rendimiento inmediato.
     */
    fun getImmediateRecommendation(currentStats: Stats): StatRecommendation {
        val baseDamage = calculateExpectedDamage(currentStats)
        if (baseDamage <= 0) {
            return StatRecommendation(
                recommendedStat = StatType.BASE_ATK,
                marginalGainPercent = 0.0,
                reason = "Las estadísticas base son insuficientes para calcular una ganancia."
            )
        }

        val gains = mutableMapOf<StatType, Double>()

        // 1. Evaluar 1 Cuadro de Prob. Crítica (+1.0%) respetando el límite del 100%
        val effectiveCR = currentStats.critRatePercent.coerceAtLeast(INNATE_CRIT_RATE)
        if (effectiveCR < 100.0) {
            val nextCritRate = min(effectiveCR + MODULE_SLOT_CRIT_RATE, 100.0)
            val withMoreCritRate = currentStats.copy(critRatePercent = nextCritRate)
            gains[StatType.CRIT_RATE] = (calculateExpectedDamage(withMoreCritRate) - baseDamage) / baseDamage
        } else {
            // Coste de oportunidad del 100%: Cualquier exceso por encima de 100% CR genera 0% ganancia
            gains[StatType.CRIT_RATE] = 0.0
        }

        // 2. Evaluar 1 Cuadro de Daño Crítico (+2.0%)
        val withMoreCritDmg = currentStats.copy(critDmgPercent = currentStats.critDmgPercent + MODULE_SLOT_CRIT_DMG)
        gains[StatType.CRIT_DMG] = (calculateExpectedDamage(withMoreCritDmg) - baseDamage) / baseDamage

        // 3. Evaluar 1 Cuadro de ATQ% (+1.25%)
        val withMoreAtkPct = currentStats.copy(atkPercent = (currentStats.atkPercent * 100 ) / currentStats.baseAtk + MODULE_SLOT_ATK_PERCENT)
        gains[StatType.ATK_PERCENT] = (calculateExpectedDamage(withMoreAtkPct) - baseDamage) / baseDamage

        // 4. Evaluar 1 Cuadro de Bono Universal (+1.0%)
        // (Nota: El Bono Elemental no existe como substat en módulos ni cartuchos)
        val plusBonusDmg = currentStats.dmgBonusPercent
        if (plusBonusDmg > 57.5) {
            val nextBonusDmg = currentStats.dmgBonusPercent + MODULE_SLOT_UNIVERSAL_DMG - (currentStats.dmgBonusPercent *((currentStats.dmgBonusPercent - 57.5) / 400) )
            val withmoreDmgBonus = currentStats.copy(dmgBonusPercent = nextBonusDmg)
            gains[StatType.DMG_BONUS] = (calculateExpectedDamage(withmoreDmgBonus) - baseDamage) / baseDamage
        } else {
            val withMoreDmgBonus =
                currentStats.copy(dmgBonusPercent = currentStats.dmgBonusPercent + MODULE_SLOT_UNIVERSAL_DMG)
            gains[StatType.DMG_BONUS] =
                (calculateExpectedDamage(withMoreDmgBonus) - baseDamage) / baseDamage
        }

        // 5. Evaluar 1 Cuadro de ATQ Plano (+8.0 pts)
        val withMoreFlatAtk = currentStats.copy(flatAtk = currentStats.flatAtk + MODULE_SLOT_FLAT_ATK)
        gains[StatType.FLAT_ATK] = (calculateExpectedDamage(withMoreFlatAtk) - baseDamage) / baseDamage

        // Ordenar ganancias para determinar el Coste de Oportunidad
        val sortedGains = gains.entries.sortedByDescending { it.value }
        val bestEntry = sortedGains.firstOrNull()
        val secondBestEntry = sortedGains.getOrNull(1)

        val bestStat = bestEntry?.key
        val bestGain = bestEntry?.value ?: 0.0
        val secondBestGain = secondBestEntry?.value ?: 0.0

        // COSTE DE OPORTUNIDAD: Ganancia extra del mejor stat frente a renunciar a la segunda mejor opción
        val opportunityCostBenefit = (bestGain - secondBestGain) * 100

        return if (bestStat != null && bestGain > 0) {
            val gainPercentFormatted = "%.2f".format(bestGain * 100)
            val diffFormatted = "%.2f".format(opportunityCostBenefit)
            val secondStatName = secondBestEntry?.key?.displayName ?: "otra opción"

            val reasonText = if (effectiveCR >= 100.0 && bestStat != StatType.CRIT_RATE) {
                "Prioriza 1 cuadro de ${bestStat.displayName} (+$gainPercentFormatted% daño). Nota: Tu Prob. Crítica ya alcanzó el 100% (incluyendo los 5% innatos); sumar más Prob. CR tendría un coste de oportunidad del 100% (desperdicio total)."
            } else {
                "Asigna ${bestStat.displayName} en tu siguiente Cuadro de Módulo (+${gainPercentFormatted}% daño estimado). Esta opción optimiza tu costo de oportunidad superando a $secondStatName por un +$diffFormatted% de eficiencia neta."
            }

            StatRecommendation(
                recommendedStat = bestStat,
                marginalGainPercent = bestGain * 100,
                reason = reasonText
            )
        } else {
            StatRecommendation(
                recommendedStat = StatType.BASE_ATK,
                marginalGainPercent = 0.0,
                reason = "Las estadísticas actuales están totalmente optimizadas."
            )
        }
    }

    fun calculateBestNextStat(currentStats: CharacterStats): StatRecommendation {
        return getImmediateRecommendation(currentStats.toStats())
    }

    private fun CharacterStats.toStats(): Stats {
        return Stats(
            baseAtk = this.baseAtk,
            atkPercent = this.atkPercent,
            flatAtk = this.flatAtk,
            // Aplicar mínimos innatos si la UI o modelo envía 0
            critRatePercent = this.critRatePercent.coerceAtLeast(INNATE_CRIT_RATE),
            critDmgPercent = this.critDmgPercent.coerceAtLeast(INNATE_CRIT_DMG),
            dmgBonusPercent = this.dmgBonusPercent
        )
    }
}