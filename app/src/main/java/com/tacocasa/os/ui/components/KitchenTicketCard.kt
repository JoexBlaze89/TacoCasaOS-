package com.tacocasa.os.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tacocasa.os.model.KitchenOrder
import com.tacocasa.os.model.OrderStatus
import com.tacocasa.os.ui.theme.TicketCompleted
import com.tacocasa.os.ui.theme.TicketInProgress
import com.tacocasa.os.ui.theme.TicketPending
import com.tacocasa.os.ui.theme.TicketReady
import com.tacocasa.os.ui.theme.TacoPrimary
import java.time.format.DateTimeFormatter

/**
 * Signature "Ticket" card component matching the web build's kitchen-ticket design
 * Displays order information in a visually distinctive card format
 */
@Composable
fun KitchenTicketCard(
    order: KitchenOrder,
    modifier: Modifier = Modifier,
    onStatusChange: ((OrderStatus) -> Unit)? = null
) {
    val backgroundColor = getTicketBackgroundColor(order.status)
    val borderColor = getTicketBorderColor(order.status)
    val statusText = order.status.toString().replace("_", " ")

    Box(
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = 3.dp,
                color = borderColor,
                shape = RoundedCornerShape(8.dp)
            )
            .background(
                color = backgroundColor,
                shape = RoundedCornerShape(8.dp)
            )
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Order header with number and status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Order #${order.orderNumber}",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = TacoPrimary
                )
                Box(
                    modifier = Modifier
                        .background(
                            color = borderColor,
                            shape = RoundedCornerShape(4.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            // Items list
            Column(
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                order.items.forEach { item ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${item.quantity}x ${item.name}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            if (item.specialRequests.isNotEmpty()) {
                                Text(
                                    text = "Special: ${item.specialRequests}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.Gray
                                )
                            }
                        }
                    }
                }
            }

            // Timing information
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = Color.Black.copy(alpha = 0.05f),
                        shape = RoundedCornerShape(4.dp)
                    )
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Estimated Time",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray
                    )
                    Text(
                        text = "${order.estimatedTime}m",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                Column {
                    Text(
                        text = "Created",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray
                    )
                    Text(
                        text = order.createdTime.format(
                            DateTimeFormatter.ofPattern("HH:mm")
                        ),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                if (order.priority > 0) {
                    Column {
                        Text(
                            text = "Priority",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Gray
                        )
                        Text(
                            text = "${order.priority}",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = TacoPrimary
                        )
                    }
                }
            }

            // Notes if present
            if (order.notes.isNotEmpty()) {
                Text(
                    text = "Notes: ${order.notes}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                    modifier = Modifier
                        .background(
                            color = Color.Yellow.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(4.dp)
                        )
                        .padding(8.dp)
                )
            }
        }
    }
}

/**
 * Get background color based on order status
 */
private fun getTicketBackgroundColor(status: OrderStatus): Color {
    return when (status) {
        OrderStatus.PENDING -> TicketPending
        OrderStatus.IN_PROGRESS -> TicketInProgress
        OrderStatus.READY -> TicketReady
        else -> TicketCompleted
    }
}

/**
 * Get border color based on order status
 */
private fun getTicketBorderColor(status: OrderStatus): Color {
    return when (status) {
        OrderStatus.PENDING -> Color(0xFF1976D2)
        OrderStatus.IN_PROGRESS -> Color(0xFFF57C00)
        OrderStatus.READY -> Color(0xFF388E3C)
        else -> Color(0xFF555555)
    }
}
