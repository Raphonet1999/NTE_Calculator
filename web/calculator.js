// Port directo de CalculatorEngine.kt (com.nte.calculator.domain)

export const StatType = {
  BASE_ATK: { key: "BASE_ATK", displayName: "Ataque Base", isPercentage: false },
  ATK_PERCENT: { key: "ATK_PERCENT", displayName: "Ataque %", isPercentage: true },
  FLAT_ATK: {
    key: "FLAT_ATK",
    displayName: "Ataque base (arma o personaje) de ser posible de lo contrario sube ataque plano",
    isPercentage: false,
  },
  CRIT_RATE: { key: "CRIT_RATE", displayName: "Prob. Crítica %", isPercentage: true },
  CRIT_DMG: { key: "CRIT_DMG", displayName: "Daño Crítico %", isPercentage: true },
  DMG_BONUS: { key: "DMG_BONUS", displayName: "Bono de Daño %", isPercentage: true },
};

// 1. Stats innatos base del personaje (sin equipamiento)
export const INNATE_CRIT_RATE = 5.0;
export const INNATE_CRIT_DMG = 50.0;

// 2. Valores unitarios de mejora por 1 cuadro de módulo
const MODULE_SLOT_ATK_PERCENT = 1.25;
const MODULE_SLOT_FLAT_ATK = 8.0;
const MODULE_SLOT_CRIT_RATE = 1.0;
const MODULE_SLOT_CRIT_DMG = 2.0;
const MODULE_SLOT_UNIVERSAL_DMG = 1.0;

const fmt = (n) => n.toFixed(2);

export function createStats(overrides = {}) {
  return {
    baseAtk: 0,
    atkPercent: 0,
    flatAtk: 0,
    critRatePercent: 0,
    critDmgPercent: 0,
    dmgBonusPercent: 0,
    ...overrides,
  };
}

/** Calcula el daño base esperado asegurando los 5% CR y 50% CD innatos. */
export function calculateExpectedDamage(stats, skillMultiplierPercent = 100.0) {
  const totalAtk = stats.baseAtk * (1 + stats.atkPercent / 100.0) + stats.flatAtk;
  const skillMultiplier = skillMultiplierPercent / 100.0;

  const finalCritRate = Math.max(stats.critRatePercent, INNATE_CRIT_RATE);
  const finalCritDmg = Math.max(stats.critDmgPercent, INNATE_CRIT_DMG);

  // Regla de oro: la Prob. Crítica no aporta daño por encima del 100%
  const effectiveCritRate = Math.min(finalCritRate, 100.0) / 100.0;
  const critFactor = 1 + effectiveCritRate * (finalCritDmg / 100.0);
  const dmgBonusFactor = 1 + stats.dmgBonusPercent / 100.0;

  return totalAtk * skillMultiplier * critFactor * dmgBonusFactor;
}

/**
 * Recomienda qué subir en el SIGUIENTE cuadro de módulo (o substat de cartucho)
 * calculando el coste de oportunidad respecto al rendimiento inmediato.
 */
export function getImmediateRecommendation(currentStats) {
  const baseDamage = calculateExpectedDamage(currentStats);
  if (!(baseDamage > 0)) {
    return {
      recommendedStat: StatType.BASE_ATK,
      marginalGainPercent: 0.0,
      reason: "Las estadísticas base son insuficientes para calcular una ganancia.",
    };
  }

  const gainOf = (overrides) =>
    (calculateExpectedDamage({ ...currentStats, ...overrides }) - baseDamage) / baseDamage;
  const gains = [];

  // 1. Prob. Crítica (+1.0%) respetando el límite del 100%
  const effectiveCR = Math.max(currentStats.critRatePercent, INNATE_CRIT_RATE);
  if (effectiveCR < 100.0) {
    const nextCritRate = Math.min(effectiveCR + MODULE_SLOT_CRIT_RATE, 100.0);
    gains.push([StatType.CRIT_RATE, gainOf({ critRatePercent: nextCritRate })]);
  } else {
    gains.push([StatType.CRIT_RATE, 0.0]);
  }

  // 2. Daño Crítico (+2.0%)
  gains.push([
    StatType.CRIT_DMG,
    gainOf({ critDmgPercent: currentStats.critDmgPercent + MODULE_SLOT_CRIT_DMG }),
  ]);

  // 3. ATQ% (+1.25%). atkPercent ya llega convertido a % (ataque verde / ATK base * 100)
  gains.push([
    StatType.ATK_PERCENT,
    gainOf({ atkPercent: currentStats.atkPercent + MODULE_SLOT_ATK_PERCENT }),
  ]);

  // 4. Bono Universal (+1.0%), con rendimiento decreciente por encima de 57.5%
  const plusBonusDmg = currentStats.dmgBonusPercent;
  if (plusBonusDmg > 57.5) {
    const nextBonusDmg =
      currentStats.dmgBonusPercent +
      MODULE_SLOT_UNIVERSAL_DMG -
      currentStats.dmgBonusPercent * ((currentStats.dmgBonusPercent - 57.5) / 400);
    gains.push([StatType.DMG_BONUS, gainOf({ dmgBonusPercent: nextBonusDmg })]);
  } else {
    gains.push([
      StatType.DMG_BONUS,
      gainOf({ dmgBonusPercent: currentStats.dmgBonusPercent + MODULE_SLOT_UNIVERSAL_DMG }),
    ]);
  }

  // 5. ATQ Plano (+8.0)
  gains.push([StatType.FLAT_ATK, gainOf({ flatAtk: currentStats.flatAtk + MODULE_SLOT_FLAT_ATK })]);

  // Ordenar ganancias (orden estable, igual que sortedByDescending en Kotlin)
  const sorted = [...gains].sort((a, b) => b[1] - a[1]);
  const [bestStat, bestGain = 0] = sorted[0] ?? [];
  const [secondStat, secondBestGain = 0] = sorted[1] ?? [];

  const opportunityCostBenefit = (bestGain - secondBestGain) * 100;

  if (bestStat && bestGain > 0) {
    const gainPercentFormatted = fmt(bestGain * 100);
    const diffFormatted = fmt(opportunityCostBenefit);
    const secondStatName = secondStat?.displayName ?? "otra opción";

    const reason =
      effectiveCR >= 100.0 && bestStat !== StatType.CRIT_RATE
        ? `Prioriza 1 cuadro de ${bestStat.displayName} (+${gainPercentFormatted}% daño). Nota: Tu Prob. Crítica ya alcanzó el 100% (incluyendo los 5% innatos); sumar más Prob. CR tendría un coste de oportunidad del 100% (desperdicio total).`
        : `Asigna ${bestStat.displayName} en tu siguiente Cuadro de Módulo (+${gainPercentFormatted}% daño estimado). Esta opción optimiza tu costo de oportunidad superando a ${secondStatName} por un +${diffFormatted}% de eficiencia neta.`;

    return { recommendedStat: bestStat, marginalGainPercent: bestGain * 100, reason };
  }

  return {
    recommendedStat: StatType.BASE_ATK,
    marginalGainPercent: 0.0,
    reason: "Las estadísticas actuales están totalmente optimizadas.",
  };
}
