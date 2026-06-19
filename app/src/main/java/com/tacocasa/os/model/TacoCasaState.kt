package com.tacocasa.os.model

import java.time.LocalDateTime

/**
 * Core state class representing the entire TacoCasa restaurant operations system.
 * This is the Kotlin/Compose port of the taco_casa_os.py Python class.
 *
 * All business logic (rush prediction, labor/food cost math, inventory thresholds,
 * alert conditions, complaint-recovery flows) operates on this state.
 */
data class TacoCasaState(
    // Operational state
    val isOpen: Boolean = false,
    val currentShift: Shift? = null,
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

    // Inventory
    val inventory: Map<String, InventoryItem> = emptyMap(),
    val inventoryAlerts: List<InventoryAlert> = emptyList(),

    // Kitchen operations
    val ordersInProgress: List<KitchenOrder> = emptyList(),
    val completedOrders: List<KitchenOrder> = emptyList(),
    val prepTasks: List<PrepTask> = emptyList(),

    // Cleaning and maintenance
    val cleaningTasks: List<CleaningTask> = emptyList(),
    val maintenanceIssues: List<MaintenanceIssue> = emptyList(),

    // Customer feedback and complaints
    val notes: List<OperationalNote> = emptyList(),
    val complaints: List<Complaint> = emptyList(),
    val recoveryActions: List<ComplaintRecoveryAction> = emptyList(),

    // Metrics and alerts
    val dailyMetrics: DailyMetrics = DailyMetrics(),
    val activeAlerts: List<OperationalAlert> = emptyList(),

    // Timestamps
    val shiftStartTime: LocalDateTime? = null,
    val lastUpdated: LocalDateTime = LocalDateTime.now()
)

/**
 * Represents a shift (morning, afternoon, night, etc.)
 */
enum class Shift {
    MORNING,
    AFTERNOON,
    EVENING,
    NIGHT,
    CLOSING
}

/**
 * Current rush level at the restaurant
 */
enum class RushLevel {
    SLOW,
    NORMAL,
    BUSY,
    RUSH,
    EXTREME_RUSH
}

/**
 * Prediction for upcoming rush periods
 */
enum class RushPrediction {
    NONE,
    LUNCH_APPROACHING,
    DINNER_APPROACHING,
    EVENT_INCOMING,
    WEEKEND_PEAK
}

/**
 * Represents a staff member
 */
data class StaffMember(
    val id: String,
    val name: String,
    val role: StaffRole,
    val hourlyRate: Double,
    val isActive: Boolean = true,
    val startTime: LocalDateTime? = null,
    val hoursWorked: Double = 0.0,
    val performance: StaffPerformance = StaffPerformance()
)

enum class StaffRole {
    MANAGER,
    COOK,
    PREP,
    CASHIER,
    RUNNER,
    CLEANER,
    SUPPORT
}

data class StaffPerformance(
    val ordersCompleted: Int = 0,
    val averageOrderTime: Double = 0.0,
    val qualityScore: Double = 5.0, // 1-5 scale
    val attendanceRate: Double = 100.0
)

/**
 * Scheduled shift for staff planning
 */
data class ScheduledShift(
    val staffMemberId: String,
    val staffName: String,
    val role: StaffRole,
    val date: String,
    val startTime: String,
    val endTime: String,
    val confirmed: Boolean = false
)

/**
 * Inventory item in the restaurant
 */
data class InventoryItem(
    val id: String,
    val name: String,
    val category: InventoryCategory,
    val currentQuantity: Double,
    val unit: String, // "pieces", "kg", "lbs", "L", etc.
    val minimumThreshold: Double,
    val maximumCapacity: Double,
    val unitCost: Double,
    val lastRestockDate: LocalDateTime? = null,
    val supplier: String? = null,
    val expiryDate: LocalDateTime? = null,
    val isLowStock: Boolean = false,
    val isOverstock: Boolean = false
)

enum class InventoryCategory {
    PROTEIN,
    VEGETABLES,
    GRAINS,
    SAUCES,
    DAIRY,
    BEVERAGES,
    SUPPLIES,
    PACKAGING,
    OTHER
}

/**
 * Alert when inventory reaches threshold levels
 */
data class InventoryAlert(
    val itemId: String,
    val itemName: String,
    val alertType: AlertType,
    val currentQuantity: Double,
    val threshold: Double,
    val timestamp: LocalDateTime = LocalDateTime.now()
)

enum class AlertType {
    LOW_STOCK,
    OUT_OF_STOCK,
    OVERSTOCK,
    NEAR_EXPIRY,
    EXPIRY_CRITICAL
}

/**
 * Kitchen order in progress
 */
data class KitchenOrder(
    val id: String,
    val orderNumber: Int,
    val items: List<OrderItem>,
    val status: OrderStatus,
    val createdTime: LocalDateTime,
    val startCookTime: LocalDateTime? = null,
    val completedTime: LocalDateTime? = null,
    val estimatedTime: Int, // minutes
    val actualTime: Int? = null, // minutes
    val priority: Int = 0, // higher = more urgent
    val notes: String = "",
    val assignedTo: String? = null, // staff member ID
    val totalCost: Double = 0.0
)

enum class OrderStatus {
    PENDING,
    IN_PROGRESS,
    READY,
    PICKED_UP,
    COMPLETED,
    CANCELLED
}

data class OrderItem(
    val name: String,
    val quantity: Int,
    val specialRequests: String = "",
    val pricePerUnit: Double
)

/**
 * Prep task (mise en place, prep work)
 */
data class PrepTask(
    val id: String,
    val name: String,
    val description: String,
    val dueTime: LocalDateTime,
    val status: TaskStatus = TaskStatus.PENDING,
    val assignedTo: String? = null,
    val completedTime: LocalDateTime? = null,
    val priority: Int = 0
)

/**
 * Cleaning task
 */
data class CleaningTask(
    val id: String,
    val name: String,
    val location: String,
    val frequency: CleaningFrequency,
    val lastCompleted: LocalDateTime? = null,
    val nextDue: LocalDateTime,
    val status: TaskStatus = TaskStatus.PENDING,
    val assignedTo: String? = null,
    val estimatedMinutes: Int,
    val notes: String = ""
)

enum class CleaningFrequency {
    HOURLY,
    EVERY_2_HOURS,
    EVERY_4_HOURS,
    DAILY,
    WEEKLY,
    MONTHLY
}

enum class TaskStatus {
    PENDING,
    IN_PROGRESS,
    COMPLETED,
    CANCELLED
}

/**
 * Maintenance issue tracking
 */
data class MaintenanceIssue(
    val id: String,
    val title: String,
    val description: String,
    val location: String,
    val severity: Severity,
    val status: MaintenanceStatus = MaintenanceStatus.OPEN,
    val reportedDate: LocalDateTime,
    val resolvedDate: LocalDateTime? = null,
    val assignedTo: String? = null
)

enum class Severity {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL
}

enum class MaintenanceStatus {
    OPEN,
    IN_PROGRESS,
    RESOLVED,
    CANCELLED
}

/**
 * Operational note (general observations, ideas, reminders)
 */
data class OperationalNote(
    val id: String,
    val content: String,
    val category: NoteCategory = NoteCategory.GENERAL,
    val timestamp: LocalDateTime = LocalDateTime.now(),
    val author: String? = null
)

enum class NoteCategory {
    GENERAL,
    ISSUE,
    OPPORTUNITY,
    REMINDER,
    INCIDENT
}

/**
 * Customer complaint
 */
data class Complaint(
    val id: String,
    val description: String,
    val severity: Severity,
    val complaintDate: LocalDateTime,
    val source: String, // "Phone", "In-person", "Online", etc.
    val status: ComplaintStatus = ComplaintStatus.NEW,
    val recoveryAttempted: Boolean = false,
    val notes: String = ""
)

enum class ComplaintStatus {
    NEW,
    ACKNOWLEDGED,
    IN_RECOVERY,
    RESOLVED,
    ESCALATED
}

/**
 * Recovery action for complaints
 */
data class ComplaintRecoveryAction(
    val id: String,
    val complaintId: String,
    val action: String,
    val dateOffered: LocalDateTime,
    val dateAccepted: LocalDateTime? = null,
    val status: RecoveryStatus = RecoveryStatus.PENDING,
    val details: String = ""
)

enum class RecoveryStatus {
    PENDING,
    ACCEPTED,
    REJECTED,
    COMPLETED
}

/**
 * Daily operational metrics
 */
data class DailyMetrics(
    val date: String = "",
    val totalOrders: Int = 0,
    val totalRevenue: Double = 0.0,
    val averageOrderValue: Double = 0.0,
    val averagePrepTime: Double = 0.0,
    val customerSatisfactionScore: Double = 5.0,
    val staffEfficiencyScore: Double = 100.0,
    val foodCostPercentage: Double = 0.0,
    val laborCostPercentage: Double = 0.0,
    val profitMargin: Double = 0.0,
    val peakHour: String = "",
    val peakHourOrders: Int = 0
)

/**
 * Operational alert (system-generated warnings)
 */
data class OperationalAlert(
    val id: String,
    val title: String,
    val message: String,
    val alertLevel: AlertLevel,
    val category: AlertCategory,
    val timestamp: LocalDateTime = LocalDateTime.now(),
    val isResolved: Boolean = false,
    val suggestedAction: String = ""
)

enum class AlertLevel {
    INFO,
    WARNING,
    CRITICAL
}

enum class AlertCategory {
    INVENTORY,
    STAFF,
    QUALITY,
    SAFETY,
    COMPLIANCE,
    FINANCIAL,
    SYSTEM
}
