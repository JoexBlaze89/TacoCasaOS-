package com.tacocasa.os.model

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

object LocalDateTimeSerializer : KSerializer<LocalDateTime> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("LocalDateTime", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: LocalDateTime) =
        encoder.encodeString(value.format(DateTimeFormatter.ISO_DATE_TIME))

    override fun deserialize(decoder: Decoder): LocalDateTime =
        LocalDateTime.parse(decoder.decodeString(), DateTimeFormatter.ISO_DATE_TIME)
}

/** Core serialized operational state for Taco Casa OS. */
@Serializable
data class TacoCasaState(
    val isOpen: Boolean = false,
    val currentShift: Shift? = null,
    val rushLevel: RushLevel = RushLevel.NORMAL,
    val rushPrediction: RushPrediction = RushPrediction.NONE,
    val totalRevenue: Double = 0.0,
    val laborCosts: Double = 0.0,
    val foodCosts: Double = 0.0,
    val otherExpenses: Double = 0.0,
    val profit: Double = 0.0,
    val activeStaff: List<StaffMember> = emptyList(),
    val staffSchedule: List<ScheduledShift> = emptyList(),
    val inventory: Map<String, InventoryItem> = emptyMap(),
    val inventoryAlerts: List<InventoryAlert> = emptyList(),
    val ordersInProgress: List<KitchenOrder> = emptyList(),
    val completedOrders: List<KitchenOrder> = emptyList(),
    val prepTasks: List<PrepTask> = emptyList(),
    val cleaningTasks: List<CleaningTask> = emptyList(),
    val maintenanceIssues: List<MaintenanceIssue> = emptyList(),
    val notes: List<OperationalNote> = emptyList(),
    val complaints: List<Complaint> = emptyList(),
    val recoveryActions: List<ComplaintRecoveryAction> = emptyList(),
    val dailyMetrics: DailyMetrics = DailyMetrics(),
    val activeAlerts: List<OperationalAlert> = emptyList(),
    @Serializable(with = LocalDateTimeSerializer::class)
    val shiftStartTime: LocalDateTime? = null,
    @Serializable(with = LocalDateTimeSerializer::class)
    val lastUpdated: LocalDateTime = LocalDateTime.now()
)

@Serializable enum class Shift { MORNING, AFTERNOON, EVENING, NIGHT, CLOSING }
@Serializable enum class RushLevel { SLOW, NORMAL, BUSY, RUSH, EXTREME_RUSH }
@Serializable enum class RushPrediction { NONE, LUNCH_APPROACHING, DINNER_APPROACHING, EVENT_INCOMING, WEEKEND_PEAK }

@Serializable
data class StaffMember(
    val id: String, val name: String, val role: StaffRole, val hourlyRate: Double,
    val isActive: Boolean = true,
    @Serializable(with = LocalDateTimeSerializer::class) val startTime: LocalDateTime? = null,
    val hoursWorked: Double = 0.0, val performance: StaffPerformance = StaffPerformance()
)
@Serializable enum class StaffRole { MANAGER, COOK, PREP, CASHIER, RUNNER, CLEANER, SUPPORT }
@Serializable data class StaffPerformance(val ordersCompleted: Int = 0, val averageOrderTime: Double = 0.0, val qualityScore: Double = 5.0, val attendanceRate: Double = 100.0)
@Serializable data class ScheduledShift(val staffMemberId: String, val staffName: String, val role: StaffRole, val date: String, val startTime: String, val endTime: String, val confirmed: Boolean = false)

@Serializable
data class InventoryItem(
    val id: String, val name: String, val category: InventoryCategory, val currentQuantity: Double,
    val unit: String, val minimumThreshold: Double, val maximumCapacity: Double, val unitCost: Double,
    @Serializable(with = LocalDateTimeSerializer::class) val lastRestockDate: LocalDateTime? = null,
    val supplier: String? = null,
    @Serializable(with = LocalDateTimeSerializer::class) val expiryDate: LocalDateTime? = null,
    val isLowStock: Boolean = false, val isOverstock: Boolean = false
)
@Serializable enum class InventoryCategory { PROTEIN, VEGETABLES, GRAINS, SAUCES, DAIRY, BEVERAGES, SUPPLIES, PACKAGING, OTHER }
@Serializable
data class InventoryAlert(
    val itemId: String, val itemName: String, val alertType: AlertType, val currentQuantity: Double, val threshold: Double,
    @Serializable(with = LocalDateTimeSerializer::class) val timestamp: LocalDateTime = LocalDateTime.now()
)
@Serializable enum class AlertType { LOW_STOCK, OUT_OF_STOCK, OVERSTOCK, NEAR_EXPIRY, EXPIRY_CRITICAL }

@Serializable
data class KitchenOrder(
    val id: String, val orderNumber: Int, val items: List<OrderItem>, val status: OrderStatus,
    @Serializable(with = LocalDateTimeSerializer::class) val createdTime: LocalDateTime,
    @Serializable(with = LocalDateTimeSerializer::class) val startCookTime: LocalDateTime? = null,
    @Serializable(with = LocalDateTimeSerializer::class) val completedTime: LocalDateTime? = null,
    val estimatedTime: Int, val actualTime: Int? = null, val priority: Int = 0, val notes: String = "",
    val assignedTo: String? = null, val totalCost: Double = 0.0
)
@Serializable enum class OrderStatus { PENDING, IN_PROGRESS, READY, PICKED_UP, COMPLETED, CANCELLED }
@Serializable data class OrderItem(val name: String, val quantity: Int, val specialRequests: String = "", val pricePerUnit: Double)

@Serializable
data class PrepTask(
    val id: String, val name: String, val description: String,
    @Serializable(with = LocalDateTimeSerializer::class) val dueTime: LocalDateTime,
    val status: TaskStatus = TaskStatus.PENDING, val assignedTo: String? = null,
    @Serializable(with = LocalDateTimeSerializer::class) val completedTime: LocalDateTime? = null,
    val priority: Int = 0
)
@Serializable
data class CleaningTask(
    val id: String, val name: String, val location: String, val frequency: CleaningFrequency,
    @Serializable(with = LocalDateTimeSerializer::class) val lastCompleted: LocalDateTime? = null,
    @Serializable(with = LocalDateTimeSerializer::class) val nextDue: LocalDateTime,
    val status: TaskStatus = TaskStatus.PENDING, val assignedTo: String? = null,
    val estimatedMinutes: Int, val notes: String = ""
)
@Serializable enum class CleaningFrequency { HOURLY, EVERY_2_HOURS, EVERY_4_HOURS, DAILY, WEEKLY, MONTHLY }
@Serializable enum class TaskStatus { PENDING, IN_PROGRESS, COMPLETED, CANCELLED }

@Serializable
data class MaintenanceIssue(
    val id: String, val title: String, val description: String, val location: String, val severity: Severity,
    val status: MaintenanceStatus = MaintenanceStatus.OPEN,
    @Serializable(with = LocalDateTimeSerializer::class) val reportedDate: LocalDateTime,
    @Serializable(with = LocalDateTimeSerializer::class) val resolvedDate: LocalDateTime? = null,
    val assignedTo: String? = null
)
@Serializable enum class Severity { LOW, MEDIUM, HIGH, CRITICAL }
@Serializable enum class MaintenanceStatus { OPEN, IN_PROGRESS, RESOLVED, CANCELLED }

@Serializable
data class OperationalNote(
    val id: String, val content: String, val category: NoteCategory = NoteCategory.GENERAL,
    @Serializable(with = LocalDateTimeSerializer::class) val timestamp: LocalDateTime = LocalDateTime.now(),
    val author: String? = null
)
@Serializable enum class NoteCategory { GENERAL, ISSUE, OPPORTUNITY, REMINDER, INCIDENT }

@Serializable
data class Complaint(
    val id: String, val description: String, val severity: Severity,
    @Serializable(with = LocalDateTimeSerializer::class) val complaintDate: LocalDateTime,
    val source: String, val status: ComplaintStatus = ComplaintStatus.NEW,
    val recoveryAttempted: Boolean = false, val notes: String = ""
)
@Serializable enum class ComplaintStatus { NEW, ACKNOWLEDGED, IN_RECOVERY, RESOLVED, ESCALATED }

@Serializable
data class ComplaintRecoveryAction(
    val id: String, val complaintId: String, val action: String,
    @Serializable(with = LocalDateTimeSerializer::class) val dateOffered: LocalDateTime,
    @Serializable(with = LocalDateTimeSerializer::class) val dateAccepted: LocalDateTime? = null,
    val status: RecoveryStatus = RecoveryStatus.PENDING, val details: String = ""
)
@Serializable enum class RecoveryStatus { PENDING, ACCEPTED, REJECTED, COMPLETED }

@Serializable
data class DailyMetrics(
    val date: String = "", val totalOrders: Int = 0, val totalRevenue: Double = 0.0,
    val averageOrderValue: Double = 0.0, val averagePrepTime: Double = 0.0,
    val customerSatisfactionScore: Double = 5.0, val staffEfficiencyScore: Double = 100.0,
    val foodCostPercentage: Double = 0.0, val laborCostPercentage: Double = 0.0,
    val profitMargin: Double = 0.0, val peakHour: String = "", val peakHourOrders: Int = 0
)

@Serializable
data class OperationalAlert(
    val id: String, val title: String, val message: String, val alertLevel: AlertLevel,
    val category: AlertCategory,
    @Serializable(with = LocalDateTimeSerializer::class) val timestamp: LocalDateTime = LocalDateTime.now(),
    val isResolved: Boolean = false, val suggestedAction: String = ""
)
@Serializable enum class AlertLevel { INFO, WARNING, CRITICAL }
@Serializable enum class AlertCategory { INVENTORY, STAFF, QUALITY, SAFETY, COMPLIANCE, FINANCIAL, SYSTEM }
