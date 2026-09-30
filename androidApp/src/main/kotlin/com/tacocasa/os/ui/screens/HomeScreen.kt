package com.tacocasa.os.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tacocasa.os.viewmodel.TacoCasaViewModel

@Composable
fun HomeScreen(
    viewModel: TacoCasaViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val state = viewModel.state.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = if (state.value.isOpen) "🟢 Restaurant Open" else "🔴 Closed",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Text(
                    text = "Shift: ${state.value.currentShift?.toString() ?: "None"}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
        }

        // Quick Stats
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatCard(
                title = "Revenue",
                value = "\$${String.format("%.2f", state.value.totalRevenue)}",
                modifier = Modifier.weight(1f)
            )
            StatCard(
                title = "Profit",
                value = "\$${String.format("%.2f", viewModel.calculateProfit())}",
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatCard(
                title = "Food Cost",
                value = "${String.format("%.1f", viewModel.getFoodCostPercentage())}%",
                modifier = Modifier.weight(1f)
            )
            StatCard(
                title = "Labor Cost",
                value = "${String.format("%.1f", viewModel.getLaborCostPercentage())}%",
                modifier = Modifier.weight(1f)
            )
        }

        // Quick Actions
        Button(
            onClick = { viewModel.startShift(com.tacocasa.os.model.Shift.MORNING) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Start Shift")
        }
        Button(
            onClick = { viewModel.endShift() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("End Shift")
        }
    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall
            )
        }
    }
}
