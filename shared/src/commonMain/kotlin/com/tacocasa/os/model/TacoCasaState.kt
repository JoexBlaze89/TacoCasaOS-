package com.tacocasa.os.model

import kotlinx.serialization.Serializable
import kotlinx.datetime.LocalDateTime

/**
 * Core state class representing the entire TacoCasa restaurant operations system.
 * This is the Kotlin/Compose port of the taco_casa_os.py Python class.
 *
 * All business logic (rush prediction, labor/food cost math, inventory thresholds,
 * alert conditions, complaint-recovery flows) operates on this state.
 * This is shared across all platforms (Android, iOS, Desktop, Web).
 */
@Serializable
data class TacoCasaState(
    // Operational state
    val isOpen: Boolean = false,
    val currentShift: Shift? = null,
    val shiftStartTime: String? = null,
    val rushLevel: RushLevel = RushLevel.NORMAL,
    val rushPrediction: RushPrediction = RushPrediction.NONE,

    // Financial tracking
    val totalRevenue: Double = 0.0,
    val laborCosts: Double = 0.0,
    val foodCosts: Double = 0.0,
    val otherExpenses: Double = 0.0,
    val profit: Double = 0.0,

    // Staff management
    val activeStaff: List<StaffMember> = emptyList(),
    val staffSchedule: List<ScheduledShift> = emptyList(),
    val staffPerformanceHistory: List<StaffPerformance> = emptyList(),

    // Inventory
    val inventory: Map<String, InventoryItem> = emptyMap(),
    val inventoryAlerts: List<InventoryAlert> = emptyList(),

    // Kitchen operations
    val ordersInProgress: List<KitchenOrder> = emptyList(),
    val completedOrders: List<KitchenOrder> = emptyList(),

    // Prep tasks
    val prepTasks: List<PrepTask> = emptyList(),

    // Cleaning & maintenance
    val cleaningTasks: List<CleaningTask> = emptyList(),
    val maintenanceIssues: List<MaintenanceIssue> = emptyList(),

    // Operational tracking
    val notes: List<OperationalNote> = emptyList(),
    val complaints: List<Complaint> = emptyList(),
    val recoveryActions: List<ComplaintRecoveryAction> = emptyList(),
    val activeAlerts: List<OperationalAlert> = emptyList(),

    // Metrics
    val dailyMetrics: DailyMetrics = DailyMetrics(),
    val lastUpdated: String = ""
)

@Serializable
enum class Shift {
    MORNING, AFTERNOON, NIGHT
}

@Serializable
enum class RushLevel {
    SLOW, NORMAL, BUSY, RUSH, EXTREME_RUSH
}

@Serializable
enum class RushPrediction {
    NONE, LUNCH_APPROACHING, DINNER_APPROACHING
}

@Serializable
data class StaffMember(
    val id: String,
    val name: String,
    val role: StaffRole,
    val hourlyRate: Double,
    val isActive: Boolean = false,
    val startTime: String? = null,
    val hoursWorked: Double = 0.0,
    val performance: StaffPerformance = StaffPerformance()
)

@Serializable
enum class StaffRole {
    MANAGER, PREP_COOK, LINE_COOK, CASHIER, DELIVERY
}

@Serializable
data class StaffPerformance(
    val id: String = "",
    val staffMemberId: String = "",
    val date: String = "",
    val ordersCompleted: Int = 0,
    val qualityScore: Double = 5.0,
    val efficiency: Double = 0.0,
    val attendance: Boolean = true
)

@Serializable
data class ScheduledShift(
    val staffMemberId: String,
    val staffName: String,
    val role: StaffRole,
    val date: String,
    val startTime: String,
    val endTime: String
)

@Serializable
data class InventoryItem(
    val id: String,
    val name: String,
    val category: InventoryCategory,
    val currentQuantity: Double,
    val unit: String,
    val minimumThreshold: Double,
    val maximumCapacity: Double,
    val unitCost: Double,
    val expiryDate: String? = null,
    val lastRestockDate: String = ""
)

@Serializable
enum class InventoryCategory {
    PRODUCE, MEAT, DAIRY, DRY_GOODS, SPICES, SUPPLIES
}

@Serializable
data class InventoryAlert(
    val id: String = "",
    val itemId: String,
    val itemName: String,
    val alertType: AlertType,
    val currentQuantity: Double,
    val threshold: Double,
    val timestamp: String = ""
)

@Serializable
enum class AlertType {
    LOW_STOCK, OUT_OF_STOCK, OVERSTOCK, NEAR_EXPIRY
}

@Serializable
data class KitchenOrder(
    val id: String,
    val orderNumber: Int,
    val items: List<OrderItem>,
    val status: OrderStatus,
    val createdTime: String = "",
    val startCookTime: String? = null,
    val completedTime: String? = null,
    val estimatedTime: Int,
    val actualTime: Int? = null,
    val priority: Int,
    val totalCost: Double
)

@Serializable
data class OrderItem(
    val id: String,
    val name: String,
    val quantity: Int,
    val pricePerUnit: Double
)

@Serializable
enum class OrderStatus {
    PENDING, IN_PROGRESS, READY, COMPLETED, CANCELLED
}

@Serializable
data class PrepTask(
    val id: String,
    val name: String,
    val description: String,
    val dueTime: String = "",
    val priority: Int,
    val status: TaskStatus = TaskStatus.PENDING,
    val completedTime: String? = null
)

@Serializable
data class CleaningTask(
    val id: String,
    val name: String,
    val location: String,
    val frequency: CleaningFrequency,
    val nextDue: String = "",
    val estimatedMinutes: Int,
    val status: TaskStatus = TaskStatus.PENDING,
    val lastCompleted: String? = null
)

@Serializable
enum class CleaningFrequency {
    HOURLY, TWICE_DAILY, DAILY, WEEKLY
}

@Serializable
enum class TaskStatus {
    PENDING, IN_PROGRESS, COMPLETED, OVERDUE
}

@Serializable
data class MaintenanceIssue(
    val id: String,
    val title: String,
    val description: String,
    val location: String,
    val severity: Severity,
    val reportedDate: String = "",
    val status: MaintenanceStatus = MaintenanceStatus.OPEN,
    val resolvedDate: String? = null
)

@Serializable
enum class MaintenanceStatus {
    OPEN, IN_PROGRESS, RESOLVED, CLOSED
}

@Serializable
enum class Severity {
    LOW, MEDIUM, HIGH, CRITICAL
}

@Serializable
data class OperationalNote(
    val id: String,
    val content: String,
    val category: NoteCategory,
    val author: String? = null,
    val timestamp: String = ""
)

@Serializable
enum class NoteCategory {
    GENERAL, WARNING, REMINDER, INCIDENT
}

@Serializable
data class Complaint(
    val id: String,
    val description: String,
    val severity: Severity,
    val complaintDate: String = "",
    val source: String,
    val status: ComplaintStatus = ComplaintStatus.FILED,
    val recoveryAttempted: Boolean = false
)

@Serializable
enum class ComplaintStatus {
    FILED, IN_RECOVERY, RESOLVED
}

@Serializable
data class ComplaintRecoveryAction(
    val id: String,
    val complaintId: String,
    val action: String,
    val dateOffered: String = "",
    val dateAccepted: String? = null,
    val details: String = "",
    val status: RecoveryStatus = RecoveryStatus.OFFERED
)

@Serializable
enum class RecoveryStatus {
    OFFERED, ACCEPTED, DECLINED, COMPLETED
}

@Serializable
data class OperationalAlert(
    val id: String,
    val title: String,
    val message: String,
    val alertLevel: AlertLevel,
    val category: AlertCategory,
    val suggestedAction: String = "",
    val isResolved: Boolean = false,
    val timestamp: String = ""
)

@Serializable
enum class AlertLevel {
    INFO, WARNING, CRITICAL
}

@Serializable
enum class AlertCategory {
    INVENTORY, STAFF, QUALITY, SAFETY, COMPLIANCE, FINANCIAL, SYSTEM
}

@Serializable
data class DailyMetrics(
    val date: String = "",
    val totalOrders: Int = 0,
    val totalRevenue: Double = 0.0,
    val averageOrderValue: Double = 0.0,
    val averagePrepTime: Double = 0.0,
    val customerSatisfactionScore: Double = 5.0,
    val staffEfficiencyScore: Double = 0.0,
    val foodCostPercentage: Double = 0.0,
    val laborCostPercentage: Double = 0.0,
    val profitMargin: Double = 0.0
)
