package com.tacocasa.os.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tacocasa.os.data.EventType
import com.tacocasa.os.data.OperationalEvent
import com.tacocasa.os.data.TacoCasaRepository
import com.tacocasa.os.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.util.UUID

class TacoCasaViewModel(private val repository: TacoCasaRepository) : ViewModel() {
    private val _state = MutableStateFlow(TacoCasaState())
    val state: StateFlow<TacoCasaState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val restored = repository.loadState()
            _state.value = restored
            var previous = restored
            _state.collectLatest { current ->
                if (current == previous) return@collectLatest
                repository.saveState(current)
                repository.appendEvent(EventLedgerDiff.event(previous, current))
                previous = current
            }
        }
    }

    private fun mutate(transform: (TacoCasaState) -> TacoCasaState) {
        viewModelScope.launch {
            _state.value = transform(_state.value).copy(lastUpdated = LocalDateTime.now())
        }
    }

    fun startShift(shift: Shift) = mutate { it.copy(isOpen = true, currentShift = shift, shiftStartTime = LocalDateTime.now()) }
    fun endShift() = mutate { it.copy(isOpen = false, currentShift = null) }

    fun predictRush(): RushPrediction = when (LocalDateTime.now().hour) {
        in 11..13 -> RushPrediction.LUNCH_APPROACHING
        in 17..19 -> RushPrediction.DINNER_APPROACHING
        else -> RushPrediction.NONE
    }

    fun updateRushLevel(ordersInQueue: Int, averagePrepTime: Double) = mutate {
        val level = when {
            ordersInQueue == 0 -> RushLevel.SLOW
            ordersInQueue in 1..3 -> RushLevel.NORMAL
            ordersInQueue in 4..7 -> RushLevel.BUSY
            ordersInQueue in 8..15 -> RushLevel.RUSH
            else -> RushLevel.EXTREME_RUSH
        }
        it.copy(rushLevel = level, rushPrediction = predictRush())
    }

    fun calculateProfit(): Double = _state.value.totalRevenue - (_state.value.laborCosts + _state.value.foodCosts + _state.value.otherExpenses)
    fun getFoodCostPercentage(): Double = if (_state.value.totalRevenue == 0.0) 0.0 else _state.value.foodCosts / _state.value.totalRevenue * 100
    fun getLaborCostPercentage(): Double = if (_state.value.totalRevenue == 0.0) 0.0 else _state.value.laborCosts / _state.value.totalRevenue * 100
    fun calculateLaborCosts(): Double = _state.value.activeStaff.sumOf { it.hoursWorked * it.hourlyRate }

    fun addRevenue(amount: Double) = mutate {
        val revenue = it.totalRevenue + amount
        it.copy(totalRevenue = revenue, profit = revenue - it.laborCosts - it.foodCosts - it.otherExpenses)
    }

    fun addExpense(amount: Double, category: ExpenseCategory) = mutate {
        val updated = when (category) {
            ExpenseCategory.LABOR -> it.copy(laborCosts = it.laborCosts + amount)
            ExpenseCategory.FOOD -> it.copy(foodCosts = it.foodCosts + amount)
            ExpenseCategory.OTHER -> it.copy(otherExpenses = it.otherExpenses + amount)
        }
        updated.copy(profit = updated.totalRevenue - updated.laborCosts - updated.foodCosts - updated.otherExpenses)
    }

    enum class ExpenseCategory { LABOR, FOOD, OTHER }

    fun clockInStaff(name: String, role: StaffRole, hourlyRate: Double) = mutate {
        it.copy(activeStaff = it.activeStaff + StaffMember(UUID.randomUUID().toString(), name, role, hourlyRate, startTime = LocalDateTime.now()))
    }

    fun clockOutStaff(staffId: String) = mutate {
        val now = LocalDateTime.now()
        it.copy(activeStaff = it.activeStaff.map { staff ->
            if (staff.id == staffId && staff.startTime != null) staff.copy(
                isActive = false,
                hoursWorked = java.time.temporal.ChronoUnit.MINUTES.between(staff.startTime, now) / 60.0
            ) else staff
        }.filter { it.isActive })
    }

    fun getActiveStaffCount(): Int = _state.value.activeStaff.count { it.isActive }

    fun scheduleShift(staffId: String, staffName: String, role: StaffRole, date: String, startTime: String, endTime: String) = mutate {
        it.copy(staffSchedule = it.staffSchedule + ScheduledShift(staffId, staffName, role, date, startTime, endTime))
    }

    fun addInventoryItem(name: String, category: InventoryCategory, quantity: Double, unit: String, minimumThreshold: Double, maximumCapacity: Double, unitCost: Double) = mutate {
        val item = InventoryItem(UUID.randomUUID().toString(), name, category, quantity, unit, minimumThreshold, maximumCapacity, unitCost, lastRestockDate = LocalDateTime.now())
        it.copy(inventory = it.inventory + (item.id to item))
    }

    fun updateInventory(itemId: String, quantityUsed: Double) = mutate {
        it.copy(inventory = it.inventory.mapValues { (id, item) -> if (id == itemId) item.copy(currentQuantity = item.currentQuantity - quantityUsed) else item })
    }

    fun restockItem(itemId: String, quantity: Double) = mutate {
        it.copy(inventory = it.inventory.mapValues { (id, item) -> if (id == itemId) item.copy(currentQuantity = item.currentQuantity + quantity, lastRestockDate = LocalDateTime.now()) else item })
    }

    fun checkInventoryThresholds() = mutate {
        val now = LocalDateTime.now()
        val alerts = it.inventory.values.flatMap { item ->
            when {
                item.currentQuantity <= 0 -> listOf(InventoryAlert(item.id, item.name, AlertType.OUT_OF_STOCK, item.currentQuantity, item.minimumThreshold))
                item.currentQuantity < item.minimumThreshold -> listOf(InventoryAlert(item.id, item.name, AlertType.LOW_STOCK, item.currentQuantity, item.minimumThreshold))
                item.currentQuantity > item.maximumCapacity -> listOf(InventoryAlert(item.id, item.name, AlertType.OVERSTOCK, item.currentQuantity, item.maximumCapacity))
                item.expiryDate != null && item.expiryDate.isBefore(now.plusDays(3)) -> listOf(InventoryAlert(item.id, item.name, AlertType.NEAR_EXPIRY, item.currentQuantity, item.minimumThreshold))
                else -> emptyList()
            }
        }
        it.copy(inventoryAlerts = alerts)
    }

    fun getLowStockItems(): List<InventoryItem> = _state.value.inventory.values.filter { it.currentQuantity < it.minimumThreshold }

    fun createOrder(items: List<OrderItem>, priority: Int = 0) = mutate {
        val order = KitchenOrder(UUID.randomUUID().toString(), it.ordersInProgress.size + it.completedOrders.size + 1, items, OrderStatus.PENDING,
            LocalDateTime.now(), estimatedTime = 5 + items.sumOf { item -> item.quantity } * 2, priority = priority,
            totalCost = items.sumOf { item -> item.quantity * item.pricePerUnit })
        it.copy(ordersInProgress = it.ordersInProgress + order)
    }

    fun startCooking(orderId: String) = mutate { state -> state.copy(ordersInProgress = state.ordersInProgress.map { order ->
        if (order.id == orderId) order.copy(status = OrderStatus.IN_PROGRESS, startCookTime = LocalDateTime.now()) else order
    }) }

    fun completeOrder(orderId: String) = mutate { state ->
        val completed = state.ordersInProgress.find { it.id == orderId }?.copy(status = OrderStatus.READY, completedTime = LocalDateTime.now())
        if (completed == null) state else state.copy(ordersInProgress = state.ordersInProgress.filter { it.id != orderId }, completedOrders = state.completedOrders + completed)
    }

    fun getOrdersByPriority(): List<KitchenOrder> = _state.value.ordersInProgress.sortedByDescending { it.priority }
    fun getAveragePrepTime(): Double = _state.value.completedOrders.filter { it.actualTime != null }.let { orders -> if (orders.isEmpty()) 0.0 else orders.map { it.actualTime ?: 0 }.average() }

    fun createPrepTask(name: String, description: String, dueTime: LocalDateTime, priority: Int = 0) = mutate {
        it.copy(prepTasks = it.prepTasks + PrepTask(UUID.randomUUID().toString(), name, description, dueTime, priority = priority))
    }

    fun completePrepTask(taskId: String) = mutate { state -> state.copy(prepTasks = state.prepTasks.map { task ->
        if (task.id == taskId) task.copy(status = TaskStatus.COMPLETED, completedTime = LocalDateTime.now()) else task
    }) }

    fun createCleaningTask(name: String, location: String, frequency: CleaningFrequency, estimatedMinutes: Int) = mutate {
        it.copy(cleaningTasks = it.cleaningTasks + CleaningTask(UUID.randomUUID().toString(), name, location, frequency, nextDue = LocalDateTime.now(), estimatedMinutes = estimatedMinutes))
    }

    fun markCleaningComplete(taskId: String) = mutate { state -> state.copy(cleaningTasks = state.cleaningTasks.map { task ->
        if (task.id == taskId) task.copy(status = TaskStatus.COMPLETED, lastCompleted = LocalDateTime.now()) else task
    }) }

    fun getOverdueCleaningTasks(): List<CleaningTask> = _state.value.cleaningTasks.filter { it.status != TaskStatus.COMPLETED && it.nextDue.isBefore(LocalDateTime.now()) }

    fun reportMaintenanceIssue(title: String, description: String, location: String, severity: Severity) = mutate {
        it.copy(maintenanceIssues = it.maintenanceIssues + MaintenanceIssue(UUID.randomUUID().toString(), title, description, location, severity, reportedDate = LocalDateTime.now()))
    }

    fun resolveMaintenanceIssue(issueId: String) = mutate { state -> state.copy(maintenanceIssues = state.maintenanceIssues.map { issue ->
        if (issue.id == issueId) issue.copy(status = MaintenanceStatus.RESOLVED, resolvedDate = LocalDateTime.now()) else issue
    }) }

    fun addNote(content: String, category: NoteCategory = NoteCategory.GENERAL, author: String? = null) = mutate {
        it.copy(notes = it.notes + OperationalNote(UUID.randomUUID().toString(), content, category, author = author))
    }

    fun fileComplaint(description: String, severity: Severity, source: String) = mutate {
        it.copy(complaints = it.complaints + Complaint(UUID.randomUUID().toString(), description, severity, LocalDateTime.now(), source))
    }

    fun offerRecovery(complaintId: String, action: String, details: String = "") = mutate { state -> state.copy(
        recoveryActions = state.recoveryActions + ComplaintRecoveryAction(UUID.randomUUID().toString(), complaintId, action, LocalDateTime.now(), details = details),
        complaints = state.complaints.map { c -> if (c.id == complaintId) c.copy(status = ComplaintStatus.IN_RECOVERY, recoveryAttempted = true) else c }
    ) }

    fun acceptRecovery(recoveryId: String) = mutate { state -> state.copy(recoveryActions = state.recoveryActions.map { recovery ->
        if (recovery.id == recoveryId) recovery.copy(status = RecoveryStatus.ACCEPTED, dateAccepted = LocalDateTime.now()) else recovery
    }) }

    fun resolveComplaint(complaintId: String) = mutate { state -> state.copy(complaints = state.complaints.map { c ->
        if (c.id == complaintId) c.copy(status = ComplaintStatus.RESOLVED, recoveryAttempted = true) else c
    }) }

    fun getUnresolvedComplaints(): List<Complaint> = _state.value.complaints.filter { it.status != ComplaintStatus.RESOLVED }

    fun addAlert(title: String, message: String, level: AlertLevel, category: AlertCategory, suggestedAction: String = "") = mutate {
        it.copy(activeAlerts = it.activeAlerts + OperationalAlert(UUID.randomUUID().toString(), title, message, level, category, suggestedAction = suggestedAction))
    }

    fun resolveAlert(alertId: String) = mutate { state -> state.copy(activeAlerts = state.activeAlerts.filter { it.id != alertId }) }
    fun getActiveAlerts(): List<OperationalAlert> = _state.value.activeAlerts.filter { !it.isResolved }

    fun computeDailyMetrics() = mutate { state ->
        val completed = state.completedOrders
        val revenue = state.totalRevenue
        val profit = revenue - state.laborCosts - state.foodCosts - state.otherExpenses
        state.copy(dailyMetrics = DailyMetrics(
            date = LocalDateTime.now().toLocalDate().toString(),
            totalOrders = completed.size,
            totalRevenue = revenue,
            averageOrderValue = if (completed.isEmpty()) 0.0 else revenue / completed.size,
            averagePrepTime = completed.filter { it.actualTime != null }.let { orders -> if (orders.isEmpty()) 0.0 else orders.map { it.actualTime ?: 0 }.average() },
            customerSatisfactionScore = 5.0,
            staffEfficiencyScore = if (state.activeStaff.isEmpty()) 100.0 else state.activeStaff.map { it.performance.qualityScore }.average(),
            foodCostPercentage = if (revenue == 0.0) 0.0 else state.foodCosts / revenue * 100,
            laborCostPercentage = if (revenue == 0.0) 0.0 else state.laborCosts / revenue * 100,
            profitMargin = if (revenue == 0.0) 0.0 else profit / revenue * 100
        ))
    }

    fun getDailySummary(): String {
        val metrics = _state.value.dailyMetrics
        return """Daily Summary:
            Total Orders: ${metrics.totalOrders}
            Revenue: $${String.format("%.2f", metrics.totalRevenue)}
            Profit: $${String.format("%.2f", calculateProfit())}
            Labor Cost %: ${String.format("%.1f", metrics.laborCostPercentage)}%
            Food Cost %: ${String.format("%.1f", metrics.foodCostPercentage)}%
            Profit Margin: ${String.format("%.1f", metrics.profitMargin)}%
        """.trimIndent()
    }
}

private object EventLedgerDiff {
    fun event(previous: TacoCasaState, current: TacoCasaState): OperationalEvent {
        val type = when {
            previous.isOpen != current.isOpen -> if (current.isOpen) EventType.SHIFT_STARTED else EventType.SHIFT_ENDED
            previous.rushLevel != current.rushLevel || previous.rushPrediction != current.rushPrediction -> EventType.RUSH_CHANGED
            previous.totalRevenue != current.totalRevenue -> EventType.REVENUE_CHANGED
            previous.laborCosts != current.laborCosts || previous.foodCosts != current.foodCosts || previous.otherExpenses != current.otherExpenses -> EventType.EXPENSE_CHANGED
            previous.activeStaff != current.activeStaff || previous.staffSchedule != current.staffSchedule -> EventType.STAFF_CHANGED
            previous.inventory != current.inventory || previous.inventoryAlerts != current.inventoryAlerts -> EventType.INVENTORY_CHANGED
            previous.ordersInProgress.size < current.ordersInProgress.size -> EventType.ORDER_CREATED
            previous.completedOrders.size < current.completedOrders.size -> EventType.ORDER_COMPLETED
            previous.ordersInProgress.map { it.id to it.status } != current.ordersInProgress.map { it.id to it.status } -> EventType.ORDER_STARTED
            previous.prepTasks != current.prepTasks -> EventType.PREP_CHANGED
            previous.cleaningTasks != current.cleaningTasks -> EventType.CLEANING_CHANGED
            previous.maintenanceIssues != current.maintenanceIssues -> EventType.MAINTENANCE_CHANGED
            previous.notes != current.notes -> EventType.NOTE_ADDED
            previous.complaints != current.complaints -> EventType.COMPLAINT_CHANGED
            previous.recoveryActions != current.recoveryActions -> EventType.RECOVERY_CHANGED
            previous.activeAlerts != current.activeAlerts -> EventType.ALERT_CHANGED
            previous.dailyMetrics != current.dailyMetrics -> EventType.METRICS_CHANGED
            else -> EventType.STATE_CHANGED
        }
        return OperationalEvent(type = type, summary = describe(type, previous, current))
    }

    private fun describe(type: EventType, previous: TacoCasaState, current: TacoCasaState): String = when (type) {
        EventType.SHIFT_STARTED -> "Shift started: ${current.currentShift}"
        EventType.SHIFT_ENDED -> "Shift ended"
        EventType.RUSH_CHANGED -> "Rush ${previous.rushLevel} -> ${current.rushLevel}; prediction=${current.rushPrediction}"
        EventType.REVENUE_CHANGED -> "Revenue ${previous.totalRevenue} -> ${current.totalRevenue}"
        EventType.EXPENSE_CHANGED -> "Operating costs changed"
        EventType.STAFF_CHANGED -> "Staff state changed; active=${current.activeStaff.size}"
        EventType.INVENTORY_CHANGED -> "Inventory changed; alerts=${current.inventoryAlerts.size}"
        EventType.ORDER_CREATED -> "Kitchen order created; queue=${current.ordersInProgress.size}"
        EventType.ORDER_STARTED -> "Kitchen order state changed"
        EventType.ORDER_COMPLETED -> "Kitchen order completed; completed=${current.completedOrders.size}"
        EventType.PREP_CHANGED -> "Prep task state changed"
        EventType.CLEANING_CHANGED -> "Cleaning task state changed"
        EventType.MAINTENANCE_CHANGED -> "Maintenance state changed"
        EventType.NOTE_ADDED -> "Operational note added"
        EventType.COMPLAINT_CHANGED -> "Complaint state changed"
        EventType.RECOVERY_CHANGED -> "Complaint recovery state changed"
        EventType.ALERT_CHANGED -> "Operational alerts changed"
        EventType.METRICS_CHANGED -> "Daily metrics changed"
        EventType.STATE_CHANGED -> "Operational state changed"
    }
}
