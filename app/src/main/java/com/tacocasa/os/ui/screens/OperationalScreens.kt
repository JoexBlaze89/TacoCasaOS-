package com.tacocasa.os.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.tacocasa.os.ui.components.KitchenTicketCard
import com.tacocasa.os.viewmodel.TacoCasaViewModel

/**
 * Prep screen - shows prep tasks and work assignments
 */
@Composable
fun PrepScreen(
    viewModel: TacoCasaViewModel,
    modifier: Modifier = Modifier
) {
    val state = viewModel.state.collectAsState()
    val prepTasks = state.value.prepTasks

    Column(
        modifier = modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Prep Tasks",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "${prepTasks.count { it.status.toString() == "PENDING" }} pending",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(prepTasks) { task ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFFFF3E0)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = task.name,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = task.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                        Button(
                            onClick = { viewModel.completePrepTask(task.id) },
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text("Mark Done")
                        }
                    }
                }
            }
        }
    }
}

/**
 * Kitchen screen - displays kitchen orders as tickets
 */
@Composable
fun KitchenScreen(
    viewModel: TacoCasaViewModel,
    modifier: Modifier = Modifier
) {
    val state = viewModel.state.collectAsState()
    val orders = viewModel.getOrdersByPriority()

    Column(
        modifier = modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Kitchen Orders",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "${orders.size} orders in queue",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(orders) { order ->
                KitchenTicketCard(
                    order = order,
                    onStatusChange = { newStatus ->
                        when (newStatus.toString()) {
                            "IN_PROGRESS" -> viewModel.startCooking(order.id)
                            "READY" -> viewModel.completeOrder(order.id)
                            else -> {}
                        }
                    }
                )
            }
        }
    }
}

/**
 * Inventory screen - displays inventory items and stock levels
 */
@Composable
fun InventoryScreen(
    viewModel: TacoCasaViewModel,
    modifier: Modifier = Modifier
) {
    val state = viewModel.state.collectAsState()
    val inventory = state.value.inventory.values.toList()
    val lowStockItems = viewModel.getLowStockItems()

    Column(
        modifier = modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Inventory",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            if (lowStockItems.isNotEmpty()) {
                Text(
                    text = "⚠️ ${lowStockItems.size} items low on stock",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFD32F2F)
                )
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(inventory) { item ->
                val isLow = lowStockItems.any { it.id == item.id }
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isLow) Color(0xFFFFEBEE) else Color(0xFFF5F5F5)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = item.name,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${String.format("%.1f", item.currentQuantity)} ${item.unit} / ${String.format("%.1f", item.maximumCapacity)} max",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isLow) Color(0xFFD32F2F) else Color.Gray
                        )
                        if (isLow) {
                            Button(
                                onClick = { viewModel.restockItem(item.id, item.maximumCapacity - item.currentQuantity) },
                                modifier = Modifier.align(Alignment.End)
                            ) {
                                Text("Restock")
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Cleaning screen - displays cleaning tasks and maintenance
 */
@Composable
fun CleaningScreen(
    viewModel: TacoCasaViewModel,
    modifier: Modifier = Modifier
) {
    val state = viewModel.state.collectAsState()
    val cleaningTasks = state.value.cleaningTasks
    val overdueCount = viewModel.getOverdueCleaningTasks().size

    Column(
        modifier = modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Cleaning & Maintenance",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            if (overdueCount > 0) {
                Text(
                    text = "⚠️ $overdueCount tasks overdue",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFD32F2F)
                )
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(cleaningTasks) { task ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFF1F8E9)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "${task.name} - ${task.location}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${task.frequency} | Est: ${task.estimatedMinutes}m",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                        Button(
                            onClick = { viewModel.markCleaningComplete(task.id) },
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text("Complete")
                        }
                    }
                }
            }
        }
    }
}

/**
 * Notes screen - operational notes and complaints
 */
@Composable
fun NotesScreen(
    viewModel: TacoCasaViewModel,
    modifier: Modifier = Modifier
) {
    val state = viewModel.state.collectAsState()
    val notes = state.value.notes
    val unresolvedComplaints = viewModel.getUnresolvedComplaints()

    Column(
        modifier = modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Notes & Feedback",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            if (unresolvedComplaints.isNotEmpty()) {
                Text(
                    text = "⚠️ ${unresolvedComplaints.size} unresolved complaints",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFD32F2F)
                )
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(notes) { note ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFFFF9C4)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                    ) {
                        Text(
                            text = note.content,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = note.category.toString(),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Gray
                        )
                    }
                }
            }
        }
    }
}
