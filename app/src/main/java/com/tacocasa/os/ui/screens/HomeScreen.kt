package com.tacocasa.os.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tacocasa.os.model.RushLevel
import com.tacocasa.os.model.Shift
import com.tacocasa.os.ui.theme.RushLevelBusy
import com.tacocasa.os.ui.theme.RushLevelExtremeRush
import com.tacocasa.os.ui.theme.RushLevelNormal
import com.tacocasa.os.ui.theme.RushLevelRush
import com.tacocasa.os.ui.theme.RushLevelSlow
import com.tacocasa.os.ui.theme.TacoPrimary
import com.tacocasa.os.viewmodel.TacoCasaViewModel

/**
 * Home screen - displays key metrics, rush level, and quick actions
 */
@Composable
fun HomeScreen(
    viewModel: TacoCasaViewModel,
    modifier: Modifier = Modifier
) {
    val state = viewModel.state.collectAsState()
    val metrics = state.value.dailyMetrics
    val rushLevel = state.value.rushLevel
    val activeAlerts = viewModel.getActiveAlerts()
    val profit = viewModel.calculateProfit()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header with shift status
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = TacoPrimary
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
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "Shift: ${state.value.currentShift?.toString() ?: "None"}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.9f)
                )
            }
        }

        // Rush Level Card
        RushLevelIndicator(rushLevel)

        // Key Metrics
        MetricsGrid(
            metrics = metrics,
            profit = profit,
            foodCostPercentage = viewModel.getFoodCostPercentage(),
            laborCostPercentage = viewModel.getLaborCostPercentage()
        )

        // Active Alerts
        if (activeAlerts.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFFFEBEE)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "⚠️ Active Alerts (${activeAlerts.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    activeAlerts.take(3).forEach { alert ->
                        Text(
                            text = "• ${alert.title}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }

        // Quick Actions
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = { viewModel.startShift(Shift.MORNING) },
                modifier = Modifier
                    .weight(1f)
                    .padding(bottom = 16.dp)
            ) {
                Text("Start Shift")
            }
            Button(
                onClick = { viewModel.endShift() },
                modifier = Modifier
                    .weight(1f)
                    .padding(bottom = 16.dp)
            ) {
                Text("End Shift")
            }
        }
    }
}

@Composable
fun RushLevelIndicator(
    rushLevel: RushLevel,
    modifier: Modifier = Modifier
) {
    val color = when (rushLevel) {
        RushLevel.SLOW -> RushLevelSlow
        RushLevel.NORMAL -> RushLevelNormal
        RushLevel.BUSY -> RushLevelBusy
        RushLevel.RUSH -> RushLevelRush
        RushLevel.EXTREME_RUSH -> RushLevelExtremeRush
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.2f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Current Rush Level",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray
                )
                Text(
                    text = rushLevel.toString(),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
            }
            // Visual indicator
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .background(
                        color = color,
                        shape = androidx.compose.foundation.shape.CircleShape
                    )
            )
        }
    }
}

@Composable
fun MetricsGrid(
    metrics: com.tacocasa.os.model.DailyMetrics,
    profit: Double,
    foodCostPercentage: Double,
    laborCostPercentage: Double,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Today's Metrics",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MetricCard(
                title = "Revenue",
                value = "$${String.format("%.0f", metrics.totalRevenue)}",
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                title = "Profit",
                value = "$${String.format("%.0f", profit)}",
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MetricCard(
                title = "Orders",
                value = metrics.totalOrders.toString(),
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                title = "Avg Prep Time",
                value = "${String.format("%.0f", metrics.averagePrepTime)}m",
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MetricCard(
                title = "Food Cost",
                value = "${String.format("%.1f", foodCostPercentage)}%",
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                title = "Labor Cost",
                value = "${String.format("%.1f", laborCostPercentage)}%",
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun MetricCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFF5F5F5)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = Color.Gray
            )
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
