package com.nte.calculator.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.nte.calculator.ui.viewmodel.CalculatorViewModel

@Composable
fun CalculatorScreen(viewModel: CalculatorViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val expectedDmg = viewModel.getExpectedDamage()
    val recommendation = viewModel.getRecommendation()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("NTE - Calculadora de Stats", style = MaterialTheme.typography.headlineMedium)

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Daño Esperado Directo: ${"%.2f".format(expectedDmg)}", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Recomendación Inmediata:", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Subir: ${recommendation.recommendedStat.displayName} (+${"%.2f".format(recommendation.marginalGainPercent)}% Daño)",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(recommendation.reason, style = MaterialTheme.typography.bodySmall)
            }
        }

        OutlinedTextField(
            value = uiState.baseAtkInput,
            onValueChange = { viewModel.updateBaseAtk(it) },
            label = { Text("ATK Base (Personaje + Arma)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = uiState.atkPercentInput,
            onValueChange = { viewModel.updateAtkPercent(it) },
            label = { Text("ATK Adicional Total (ataque verde)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = uiState.flatAtkInput,
            onValueChange = { viewModel.updateFlatAtk(it) },
            label = { Text("ATK Plano (Módulos/Artefactos)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = uiState.critRateInput,
            onValueChange = { viewModel.updateCritRate(it) },
            label = { Text("Probabilidad CRIT %") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = uiState.critDmgInput,
            onValueChange = { viewModel.updateCritDmg(it) },
            label = { Text("Daño CRIT %") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = uiState.dmgBonusInput,
            onValueChange = { viewModel.updateDmgBonus(it) },
            label = { Text("Bono de Daño % (Elemental/Universal)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth()
        )
    }
}