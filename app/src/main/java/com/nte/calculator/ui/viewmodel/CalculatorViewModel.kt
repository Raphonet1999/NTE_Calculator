package com.nte.calculator.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.nte.calculator.data.models.StatRecommendation
import com.nte.calculator.data.models.Stats
import com.nte.calculator.domain.CalculatorEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class CalculatorUiState(
    val baseAtkInput: String = "",
    val atkPercentInput: String = "",
    val flatAtkInput: String = "",
    val critRateInput: String = "",
    val critDmgInput: String = "",
    val dmgBonusInput: String = "",
    val stats: Stats = Stats()
)

class CalculatorViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(CalculatorUiState())
    val uiState: StateFlow<CalculatorUiState> = _uiState.asStateFlow()

    private fun cleanInput(input: String): String? {
        val normalized = input.replace(',', '.')
        if (normalized.isEmpty()) return ""
        val regex = Regex("""^\d*\.?\d*$""")
        return if (regex.matches(normalized)) normalized else null
    }

    fun updateBaseAtk(value: String) {
        val cleaned = cleanInput(value) ?: return
        _uiState.update { state ->
            val num = cleaned.toDoubleOrNull() ?: 0.0
            state.copy(baseAtkInput = cleaned, stats = state.stats.copy(baseAtk = num))
        }
    }

    fun updateAtkPercent(value: String) {
        val cleaned = cleanInput(value) ?: return
        _uiState.update { state ->
            val num = cleaned.toDoubleOrNull() ?: 0.0
            state.copy(atkPercentInput = cleaned, stats = state.stats.copy(atkPercent = (num / state.stats.baseAtk) * 100))
        }
    }

    fun updateFlatAtk(value: String) {
        val cleaned = cleanInput(value) ?: return
        _uiState.update { state ->
            val num = cleaned.toDoubleOrNull() ?: 0.0
            state.copy(flatAtkInput = cleaned, stats = state.stats.copy(flatAtk = num))
        }
    }

    fun updateCritRate(value: String) {
        val cleaned = cleanInput(value) ?: return
        _uiState.update { state ->
            val num = cleaned.toDoubleOrNull() ?: 0.0
            state.copy(critRateInput = cleaned, stats = state.stats.copy(critRatePercent = num))
        }
    }

    fun updateCritDmg(value: String) {
        val cleaned = cleanInput(value) ?: return
        _uiState.update { state ->
            val num = cleaned.toDoubleOrNull() ?: 0.0
            state.copy(critDmgInput = cleaned, stats = state.stats.copy(critDmgPercent = num))
        }
    }

    fun updateDmgBonus(value: String) {
        val cleaned = cleanInput(value) ?: return
        _uiState.update { state ->
            val num = cleaned.toDoubleOrNull() ?: 0.0
            state.copy(dmgBonusInput = cleaned, stats = state.stats.copy(dmgBonusPercent = num))
        }
    }

    fun getExpectedDamage(): Double = CalculatorEngine.calculateExpectedDamage(_uiState.value.stats)
    fun getRecommendation(): StatRecommendation = CalculatorEngine.getImmediateRecommendation(_uiState.value.stats)
}
