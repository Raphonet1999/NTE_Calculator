// Equivalente web de CalculatorViewModel.kt + CalculatorScreen.kt
import { createStats, calculateExpectedDamage, getImmediateRecommendation } from "./calculator.js";

const STORAGE_KEY = "nte-calculator-inputs";
const fields = ["baseAtk", "atkGreen", "flatAtk", "critRate", "critDmg", "dmgBonus"];
const inputs = Object.fromEntries(fields.map((id) => [id, document.getElementById(id)]));
const lastValid = Object.fromEntries(fields.map((id) => [id, ""]));

const $damage = document.getElementById("damage");
const $recStat = document.getElementById("rec-stat");
const $recGain = document.getElementById("rec-gain");
const $recReason = document.getElementById("rec-reason");

// Acepta solo números decimales (coma o punto), igual que cleanInput()
function cleanInput(value) {
  const normalized = value.replace(/,/g, ".");
  if (normalized === "") return "";
  return /^\d*\.?\d*$/.test(normalized) ? normalized : null;
}

const toNum = (s) => {
  const n = parseFloat(s);
  return Number.isFinite(n) ? n : 0;
};

function buildStats() {
  const baseAtk = toNum(lastValid.baseAtk);
  const atkGreen = toNum(lastValid.atkGreen);
  return createStats({
    baseAtk,
    // El "ataque verde" se convierte a ATQ% respecto al ATK base
    atkPercent: baseAtk > 0 ? (atkGreen / baseAtk) * 100 : 0,
    flatAtk: toNum(lastValid.flatAtk),
    critRatePercent: toNum(lastValid.critRate),
    critDmgPercent: toNum(lastValid.critDmg),
    dmgBonusPercent: toNum(lastValid.dmgBonus),
  });
}

function render() {
  const stats = buildStats();
  const dmg = calculateExpectedDamage(stats);
  const rec = getImmediateRecommendation(stats);
  $damage.textContent = (Number.isFinite(dmg) ? dmg : 0).toFixed(2);
  $recStat.textContent = rec.recommendedStat.displayName;
  $recGain.textContent = `+${(Number.isFinite(rec.marginalGainPercent) ? rec.marginalGainPercent : 0).toFixed(2)}% Daño`;
  $recReason.textContent = rec.reason;
}

function save() {
  try {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(lastValid));
  } catch {}
}

function restore() {
  try {
    const saved = JSON.parse(localStorage.getItem(STORAGE_KEY) || "{}");
    for (const id of fields) {
      const cleaned = typeof saved[id] === "string" ? cleanInput(saved[id]) : null;
      if (cleaned !== null) {
        lastValid[id] = cleaned;
        inputs[id].value = cleaned;
      }
    }
  } catch {}
}

for (const id of fields) {
  inputs[id].addEventListener("input", (e) => {
    const cleaned = cleanInput(e.target.value);
    if (cleaned === null) {
      e.target.value = lastValid[id]; // rechaza caracteres inválidos
    } else {
      lastValid[id] = cleaned;
      e.target.value = cleaned;
    }
    save();
    render();
  });
}

document.getElementById("reset").addEventListener("click", () => {
  for (const id of fields) {
    lastValid[id] = "";
    inputs[id].value = "";
  }
  save();
  render();
  inputs.baseAtk.focus();
});

restore();
render();
