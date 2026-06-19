package com.tacocasa.os.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tacocasa.os.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.util.UUID
import kotlin.math.abs

/**
 * ViewModel for TacoCasaOS. Implements the business logic ported from taco_casa_os.py.
 * Each public method corresponds to a user action/screen in the app.
 *
 * This ViewModel manages:
 * - Financial calculations (rush prediction, labor/food cost math)
 * - Inventory management and threshold alerts
 * - Staff clock in/out and performance tracking
 * - Kitchen order lifecycle
 * - Cleaning and maintenance tasks
 * - Customer complaints and recovery workflows
 * - Daily operational metrics
 */
class TacoCasaViewModel : ViewModel() {

    private val _state = MutableStateFlow(TacoCasaState())
    val state: StateFlow<TacoCasaState> = _state.asStateFlow()

    // ==================== SHIFT MANAGEMENT ====================

    /**
     * Port of: start_shift()
     * Opens the restaurant and initializes a new shift
     */
    fun startShift(shift: Shift) {
        viewModelScope.launch {
            val currentState = _state.value
            _state.value = currentState.copy(
                isOpen = true,
                currentShift = shift,
                shiftStartTime = LocalDateTime.now(),
                lastUpdated = LocalDateTime.now()
            )
        }
    }

    /**
     * Port of: end_shift()
     * Closes the restaurant and finalizes the shift
     */
    fun endShift() {
        viewModelScope.launch {
            val currentState = _state.value
            _state.value = currentState.copy(
                isOpen = false,
                currentShift = null,
                lastUpdated = LocalDateTime.now()
            )
        }
    }

    // ==================== RUSH PREDICTION ====================

    /**
     * Port of: predict_rush()
     * Predicts upcoming rush based on time and other factors
     */
    fun predictRush(): RushPrediction {
        val hour = LocalDateTime.now().hour
        return when (hour) {
            in 11..13 -> RushPrediction.LUNCH_APPROACHING
            in 17..19 -> RushPrediction.DINNER_APPROACHING
            else -> RushPrediction.NONE
        }
    }

    /**
     * Port of: update_rush_level()
     * Updates the current rush level based on orders and time
     */
    fun updateRushLevel(ordersInQueue: Int, averagePrepTime: Double) {
        val rushLevel = when {
            ordersInQueue == 0 -> RushLevel.SLOW
            ordersInQueue in 1..3 -> RushLevel.NORMAL
            ordersInQueue in 4..7 -> RushLevel.BUSY
            ordersInQueue in 8..15 -> RushLevel.RUSH
            else -> RushLevel.EXTREME_RUSH
        }

        viewModelScope.launch {
            val currentState = _state.value
            val prediction = predictRush()
            _state.value = currentState.copy(
                rushLevel = rushLevel,
                rushPrediction = prediction,
                lastUpdated = LocalDateTime.now()
            )
        }
    }

    // ==================== FINANCIAL CALCULATIONS ====================

    /**
     * Port of: calculate_profit()
     * Profit = Revenue - (Labor Costs + Food Costs + Other Expenses)
     */
    fun calculateProfit(): Double {
        val state = _state.value
        return state.totalRevenue - (state.laborCosts + state.foodCosts + state.otherExpenses)
    }

    /**
     * Port of: get_food_cost_percentage()
     * Returns food costs as percentage of revenue
     */
    fun getFoodCostPercentage(): Double {
        val state = _state.value
        if (state.totalRevenue == 0.0) return 0.0
        return (state.foodCosts / state.totalRevenue) * 100
    }

    /**
     * Port of: get_labor_cost_percentage()
     * Returns labor costs as percentage of revenue
     */
    fun getLaborCostPercentage(): Double {
        val state = _state.value
        if (state.totalRevenue == 0.0) return 0.0
        return (state.laborCosts / state.totalRevenue) * 100
    }

    /**
     * Port of: calculate_labor_costs()
     * Sums all active staff hours * hourly rates
     */
    fun calculateLaborCosts(): Double {
        val state = _state.value
        return state.activeStaff.sumOf { staff ->
            staff.hoursWorked * staff.hourlyRate
        }
    }

    /**
     * Port of: add_revenue(amount)
     * Records a sale
     */
    fun addRevenue(amount: Double) {
        viewModelScope.launch {
            val currentState = _state.value
            val newRevenue = currentState.totalRevenue + amount
            _state.value = currentState.copy(
                totalRevenue = newRevenue,
                profit = calculateProfit(),
                lastUpdated = LocalDateTime.now()
            )
        }
    }

    /**
     * Port of: add_expense(amount, category)
     * Records an expense
     */
    fun addExpense(amount: Double, category: ExpenseCategory) {
        viewModelScope.launch {
            val currentState = _state.value
            val updatedState = when (category) {
                ExpenseCategory.LABOR -> currentState.copy(laborCosts = currentState.laborCosts + amount)
                ExpenseCategory.FOOD -> currentState.copy(foodCosts = currentState.foodCosts + amount)
                ExpenseCategory.OTHER -> currentState.copy(otherExpenses = currentState.otherExpenses + amount)
            }
            _state.value = updatedState.copy(
                profit = calculateProfit(),
                lastUpdated = LocalDateTime.now()
            )
        }
    }

    enum class ExpenseCategory {
        LABOR, FOOD, OTHER
    }

    // ==================== STAFF MANAGEMENT ====================

    /**
     * Port of: clock_in_staff(staff_name, role, hourly_rate)
     * Records staff clock-in time
     */
    fun clockInStaff(name: String, role: StaffRole, hourlyRate: Double) {
        viewModelScope.launch {
            val currentState = _state.value
            val staff = StaffMember(
                id = UUID.randomUUID().toString(),
                name = name,
                role = role,
                hourlyRate = hourlyRate,
                isActive = true,
                startTime = LocalDateTime.now()
            )
            _state.value = currentState.copy(
                activeStaff = currentState.activeStaff + staff,
                lastUpdated = LocalDateTime.now()
            )
        }
    }

    /**
     * Port of: clock_out_staff(staff_id)
     * Records staff clock-out time and calculates hours worked
     */
    fun clockOutStaff(staffId: String) {
        viewModelScope.launch {
            val currentState = _state.value
            val updatedStaff = currentState.activeStaff.map { staff ->
                if (staff.id == staffId && staff.startTime != null) {
                    val hoursWorked = java.time.temporal.ChronoUnit.MINUTES
                        .between(staff.startTime, LocalDateTime.now()) / 60.0
                    staff.copy(isActive = false, hoursWorked = hoursWorked)
                } else {
                    staff
                }
            }
            _state.value = currentState.copy(
                activeStaff = updatedStaff.filter { it.isActive },
                lastUpdated = LocalDateTime.now()
            )
        }
    }

    /**
     * Port of: get_active_staff_count()
     * Returns number of currently clocked-in staff
     */
    fun getActiveStaffCount(): Int {
        return _state.value.activeStaff.count { it.isActive }
    }

    /**
     * Port of: schedule_shift(staff_id, date, start_time, end_time)
     * Schedules a future shift for staff
     */
    fun scheduleShift(staffId: String, staffName: String, role: StaffRole, date: String, startTime: String, endTime: String) {
        viewModelScope.launch {
            val currentState = _state.value
            val shift = ScheduledShift(
                staffMemberId = staffId,
                staffName = staffName,
                role = role,
                date = date,
                startTime = startTime,
                endTime = endTime
            )
            _state.value = currentState.copy(
                staffSchedule = currentState.staffSchedule + shift,
                lastUpdated = LocalDateTime.now()
            )
        }
    }

    // ==================== INVENTORY MANAGEMENT ====================

    /**
     * Port of: add_inventory_item(name, category, quantity, unit, cost)
     * Adds a new inventory item
     */
    fun addInventoryItem(
        name: String,
        category: InventoryCategory,
        quantity: Double,
        unit: String,
        minimumThreshold: Double,
        maximumCapacity: Double,
        unitCost: Double
    ) {
        viewModelScope.launch {
            val currentState = _state.value
            val item = InventoryItem(
                id = UUID.randomUUID().toString(),
                name = name,
                category = category,
                currentQuantity = quantity,
                unit = unit,
                minimumThreshold = minimumThreshold,
                maximumCapacity = maximumCapacity,
                unitCost = unitCost,
                lastRestockDate = LocalDateTime.now()
            )
            val updatedInventory = currentState.inventory.toMutableMap()
            updatedInventory[item.id] = item
            _state.value = currentState.copy(
                inventory = updatedInventory,
                lastUpdated = LocalDateTime.now()
            )
            checkInventoryThresholds()
        }
    }

    /**
     * Port of: update_inventory(item_id, quantity_used)
     * Decrements inventory when items are used
     */
    fun updateInventory(itemId: String, quantityUsed: Double) {
        viewModelScope.launch {
            val currentState = _state.value
            val updatedInventory = currentState.inventory.toMutableMap()
            updatedInventory[itemId]?.let { item ->
                updatedInventory[itemId] = item.copy(
                    currentQuantity = item.currentQuantity - quantityUsed
                )
            }
            _state.value = currentState.copy(
                inventory = updatedInventory,
                lastUpdated = LocalDateTime.now()
            )
            checkInventoryThresholds()
        }
    }

    /**
     * Port of: restock_item(item_id, quantity)
     * Increments inventory for restocked items
     */
    fun restockItem(itemId: String, quantity: Double) {
        viewModelScope.launch {
            val currentState = _state.value
            val updatedInventory = currentState.inventory.toMutableMap()
            updatedInventory[itemId]?.let { item ->
                updatedInventory[itemId] = item.copy(
                    currentQuantity = item.currentQuantity + quantity,
                    lastRestockDate = LocalDateTime.now()
                )
            }
            _state.value = currentState.copy(
                inventory = updatedInventory,
                lastUpdated = LocalDateTime.now()
            )
            checkInventoryThresholds()
        }
    }

    /**
     * Port of: check_inventory_thresholds()
     * Generates alerts for low stock, overstock, or near-expiry items
     */
    fun checkInventoryThresholds() {
        viewModelScope.launch {
            val currentState = _state.value
            val alerts = mutableListOf<InventoryAlert>()
            val now = LocalDateTime.now()

            currentState.inventory.forEach { (_, item) ->
                when {
                    item.currentQuantity <= 0 -> alerts.add(
                        InventoryAlert(
                            itemId = item.id,
                            itemName = item.name,
                            alertType = AlertType.OUT_OF_STOCK,
                            currentQuantity = item.currentQuantity,
                            threshold = item.minimumThreshold
                        )
                    )
                    item.currentQuantity < item.minimumThreshold -> alerts.add(
                        InventoryAlert(
                            itemId = item.id,
                            itemName = item.name,
                            alertType = AlertType.LOW_STOCK,
                            currentQuantity = item.currentQuantity,
                            threshold = item.minimumThreshold
                        )
                    )
                    item.currentQuantity > item.maximumCapacity -> alerts.add(
                        InventoryAlert(
                            itemId = item.id,
                            itemName = item.name,
                            alertType = AlertType.OVERSTOCK,
                            currentQuantity = item.currentQuantity,
                            threshold = item.maximumCapacity
                        )
                    )
                    item.expiryDate != null && item.expiryDate.isBefore(now.plusDays(3)) -> alerts.add(
                        InventoryAlert(
                            itemId = item.id,
                            itemName = item.name,
                            alertType = AlertType.NEAR_EXPIRY,
                            currentQuantity = item.currentQuantity,
                            threshold = item.minimumThreshold
                        )
                    )
                }
            }
            _state.value = currentState.copy(
                inventoryAlerts = alerts,
                lastUpdated = LocalDateTime.now()
            )
        }
    }

    /**
     * Port of: get_low_stock_items()
     * Returns list of items below minimum threshold
     */
    fun getLowStockItems(): List<InventoryItem> {
        return _state.value.inventory.values.filter { item ->
            item.currentQuantity < item.minimumThreshold
        }
    }

    // ==================== KITCHEN OPERATIONS ====================

    /**
     * Port of: create_order(items, priority)
     * Creates a new kitchen order
     */
    fun createOrder(items: List<OrderItem>, priority: Int = 0) {
        viewModelScope.launch {
            val currentState = _state.value
            val totalCost = items.sumOf { it.quantity * it.pricePerUnit }
            val order = KitchenOrder(
                id = UUID.randomUUID().toString(),
                orderNumber = currentState.ordersInProgress.size + currentState.completedOrders.size + 1,
                items = items,
                status = OrderStatus.PENDING,
                createdTime = LocalDateTime.now(),
                estimatedTime = calculateEstimatedTime(items),
                priority = priority,
                totalCost = totalCost
            )
            _state.value = currentState.copy(
                ordersInProgress = currentState.ordersInProgress + order,
                lastUpdated = LocalDateTime.now()
            )
            updateRushLevel(currentState.ordersInProgress.size + 1, 0.0)
        }
    }

    /**
     * Port of: start_cooking(order_id)
     * Transitions an order to IN_PROGRESS
     */
    fun startCooking(orderId: String) {
        viewModelScope.launch {
            val currentState = _state.value
            val updatedOrders = currentState.ordersInProgress.map { order ->
                if (order.id == orderId) {
                    order.copy(
                        status = OrderStatus.IN_PROGRESS,
                        startCookTime = LocalDateTime.now()
                    )
                } else {
                    order
                }
            }
            _state.value = currentState.copy(
                ordersInProgress = updatedOrders,
                lastUpdated = LocalDateTime.now()
            )
        }
    }

    /**
     * Port of: complete_order(order_id)
     * Marks an order as READY
     */
    fun completeOrder(orderId: String) {
        viewModelScope.launch {
            val currentState = _state.value
            val completedOrder = currentState.ordersInProgress.find { it.id == orderId }?.copy(
                status = OrderStatus.READY,
                completedTime = LocalDateTime.now()
            )

            if (completedOrder != null) {
                _state.value = currentState.copy(
                    ordersInProgress = currentState.ordersInProgress.filter { it.id != orderId },
                    completedOrders = currentState.completedOrders + completedOrder,
                    lastUpdated = LocalDateTime.now()
                )
            }
        }
    }

    /**
     * Port of: calculate_estimated_time(items)
     * Estimates cooking time based on item count and type
     */
    private fun calculateEstimatedTime(items: List<OrderItem>): Int {
        // Base time + time per item
        return 5 + (items.sumOf { it.quantity } * 2)
    }

    /**
     * Port of: get_orders_by_priority()
     * Returns orders sorted by priority (highest first)
     */
    fun getOrdersByPriority(): List<KitchenOrder> {
        return _state.value.ordersInProgress.sortedByDescending { it.priority }
    }

    /**
     * Port of: get_average_prep_time()
     * Calculates average time to complete orders
     */
    fun getAveragePrepTime(): Double {
        val completedOrders = _state.value.completedOrders.filter { it.actualTime != null }
        if (completedOrders.isEmpty()) return 0.0
        return completedOrders.map { it.actualTime ?: 0 }.average().toDouble()
    }

    // ==================== PREP TASKS ====================

    /**
     * Port of: create_prep_task(name, description, due_time, priority)
     * Creates a new prep task
     */
    fun createPrepTask(name: String, description: String, dueTime: LocalDateTime, priority: Int = 0) {
        viewModelScope.launch {
            val currentState = _state.value
            val task = PrepTask(
                id = UUID.randomUUID().toString(),
                name = name,
                description = description,
                dueTime = dueTime,
                priority = priority
            )
            _state.value = currentState.copy(
                prepTasks = currentState.prepTasks + task,
                lastUpdated = LocalDateTime.now()
            )
        }
    }

    /**
     * Port of: complete_prep_task(task_id)
     * Marks a prep task as completed
     */
    fun completePrepTask(taskId: String) {
        viewModelScope.launch {
            val currentState = _state.value
            val updatedTasks = currentState.prepTasks.map { task ->
                if (task.id == taskId) {
                    task.copy(
                        status = TaskStatus.COMPLETED,
                        completedTime = LocalDateTime.now()
                    )
                } else {
                    task
                }
            }
            _state.value = currentState.copy(
                prepTasks = updatedTasks,
                lastUpdated = LocalDateTime.now()
            )
        }
    }

    // ==================== CLEANING TASKS ====================

    /**
     * Port of: create_cleaning_task(name, location, frequency, estimated_minutes)
     * Creates a new cleaning task
     */
    fun createCleaningTask(
        name: String,
        location: String,
        frequency: CleaningFrequency,
        estimatedMinutes: Int
    ) {
        viewModelScope.launch {
            val currentState = _state.value
            val task = CleaningTask(
                id = UUID.randomUUID().toString(),
                name = name,
                location = location,
                frequency = frequency,
                nextDue = LocalDateTime.now(),
                estimatedMinutes = estimatedMinutes
            )
            _state.value = currentState.copy(
                cleaningTasks = currentState.cleaningTasks + task,
                lastUpdated = LocalDateTime.now()
            )
        }
    }

    /**
     * Port of: mark_cleaning_complete(task_id)
     * Marks a cleaning task as completed
     */
    fun markCleaningComplete(taskId: String) {
        viewModelScope.launch {
            val currentState = _state.value
            val updatedTasks = currentState.cleaningTasks.map { task ->
                if (task.id == taskId) {
                    task.copy(
                        status = TaskStatus.COMPLETED,
                        lastCompleted = LocalDateTime.now()
                    )
                } else {
                    task
                }
            }
            _state.value = currentState.copy(
                cleaningTasks = updatedTasks,
                lastUpdated = LocalDateTime.now()
            )
        }
    }

    /**
     * Port of: get_overdue_cleaning_tasks()
     * Returns cleaning tasks that are past due
     */
    fun getOverdueCleaningTasks(): List<CleaningTask> {
        val now = LocalDateTime.now()
        return _state.value.cleaningTasks.filter { task ->
            task.status != TaskStatus.COMPLETED && task.nextDue.isBefore(now)
        }
    }

    // ==================== MAINTENANCE ====================

    /**
     * Port of: report_maintenance_issue(title, description, location, severity)
     * Reports a maintenance issue
     */
    fun reportMaintenanceIssue(
        title: String,
        description: String,
        location: String,
        severity: Severity
    ) {
        viewModelScope.launch {
            val currentState = _state.value
            val issue = MaintenanceIssue(
                id = UUID.randomUUID().toString(),
                title = title,
                description = description,
                location = location,
                severity = severity,
                reportedDate = LocalDateTime.now()
            )
            _state.value = currentState.copy(
                maintenanceIssues = currentState.maintenanceIssues + issue,
                lastUpdated = LocalDateTime.now()
            )
            // Generate alert for critical issues
            if (severity == Severity.CRITICAL) {
                addAlert(
                    title = "Critical Maintenance Issue",
                    message = "$title at $location",
                    level = AlertLevel.CRITICAL,
                    category = AlertCategory.SAFETY
                )
            }
        }
    }

    /**
     * Port of: resolve_maintenance_issue(issue_id)
     * Marks a maintenance issue as resolved
     */
    fun resolveMaintenanceIssue(issueId: String) {
        viewModelScope.launch {
            val currentState = _state.value
            val updatedIssues = currentState.maintenanceIssues.map { issue ->
                if (issue.id == issueId) {
                    issue.copy(
                        status = MaintenanceStatus.RESOLVED,
                        resolvedDate = LocalDateTime.now()
                    )
                } else {
                    issue
                }
            }
            _state.value = currentState.copy(
                maintenanceIssues = updatedIssues,
                lastUpdated = LocalDateTime.now()
            )
        }
    }

    // ==================== NOTES ====================

    /**
     * Port of: add_note(content, category, author)
     * Adds an operational note
     */
    fun addNote(content: String, category: NoteCategory = NoteCategory.GENERAL, author: String? = null) {
        viewModelScope.launch {
            val currentState = _state.value
            val note = OperationalNote(
                id = UUID.randomUUID().toString(),
                content = content,
                category = category,
                author = author
            )
            _state.value = currentState.copy(
                notes = currentState.notes + note,
                lastUpdated = LocalDateTime.now()
            )
        }
    }

    // ==================== COMPLAINTS & RECOVERY ====================

    /**
     * Port of: file_complaint(description, severity, source)
     * Files a customer complaint
     */
    fun fileComplaint(description: String, severity: Severity, source: String) {
        viewModelScope.launch {
            val currentState = _state.value
            val complaint = Complaint(
                id = UUID.randomUUID().toString(),
                description = description,
                severity = severity,
                complaintDate = LocalDateTime.now(),
                source = source
            )
            _state.value = currentState.copy(
                complaints = currentState.complaints + complaint,
                lastUpdated = LocalDateTime.now()
            )
            // Alert for high-severity complaints
            if (severity == Severity.CRITICAL) {
                addAlert(
                    title = "Critical Customer Complaint",
                    message = description,
                    level = AlertLevel.WARNING,
                    category = AlertCategory.QUALITY
                )
            }
        }
    }

    /**
     * Port of: offer_recovery(complaint_id, recovery_action, details)
     * Offers a recovery action for a complaint
     */
    fun offerRecovery(complaintId: String, action: String, details: String = "") {
        viewModelScope.launch {
            val currentState = _state.value
            val recovery = ComplaintRecoveryAction(
                id = UUID.randomUUID().toString(),
                complaintId = complaintId,
                action = action,
                dateOffered = LocalDateTime.now(),
                details = details
            )
            _state.value = currentState.copy(
                recoveryActions = currentState.recoveryActions + recovery,
                lastUpdated = LocalDateTime.now()
            )
            // Update complaint status
            updateComplaintStatus(complaintId, ComplaintStatus.IN_RECOVERY)
        }
    }

    /**
     * Port of: accept_recovery(recovery_id)
     * Marks recovery action as accepted
     */
    fun acceptRecovery(recoveryId: String) {
        viewModelScope.launch {
            val currentState = _state.value
            val updatedRecoveries = currentState.recoveryActions.map { recovery ->
                if (recovery.id == recoveryId) {
                    recovery.copy(
                        status = RecoveryStatus.ACCEPTED,
                        dateAccepted = LocalDateTime.now()
                    )
                } else {
                    recovery
                }
            }
            _state.value = currentState.copy(
                recoveryActions = updatedRecoveries,
                lastUpdated = LocalDateTime.now()
            )
        }
    }

    /**
     * Port of: resolve_complaint(complaint_id)
     * Marks complaint as resolved
     */
    fun resolveComplaint(complaintId: String) {
        viewModelScope.launch {
            updateComplaintStatus(complaintId, ComplaintStatus.RESOLVED)
        }
    }

    private suspend fun updateComplaintStatus(complaintId: String, status: ComplaintStatus) {
        val currentState = _state.value
        val updatedComplaints = currentState.complaints.map { complaint ->
            if (complaint.id == complaintId) {
                complaint.copy(status = status, recoveryAttempted = true)
            } else {
                complaint
            }
        }
        _state.value = currentState.copy(
            complaints = updatedComplaints,
            lastUpdated = LocalDateTime.now()
        )
    }

    /**
     * Port of: get_unresolved_complaints()
     * Returns complaints that haven't been resolved
     */
    fun getUnresolvedComplaints(): List<Complaint> {
        return _state.value.complaints.filter { it.status != ComplaintStatus.RESOLVED }
    }

    // ==================== ALERTS ====================

    /**
     * Port of: add_alert(title, message, level, category, suggested_action)
     * Adds an operational alert
     */
    fun addAlert(
        title: String,
        message: String,
        level: AlertLevel,
        category: AlertCategory,
        suggestedAction: String = ""
    ) {
        viewModelScope.launch {
            val currentState = _state.value
            val alert = OperationalAlert(
                id = UUID.randomUUID().toString(),
                title = title,
                message = message,
                alertLevel = level,
                category = category,
                suggestedAction = suggestedAction
            )
            _state.value = currentState.copy(
                activeAlerts = currentState.activeAlerts + alert,
                lastUpdated = LocalDateTime.now()
            )
        }
    }

    /**
     * Port of: resolve_alert(alert_id)
     * Marks an alert as resolved
     */
    fun resolveAlert(alertId: String) {
        viewModelScope.launch {
            val currentState = _state.value
            val updatedAlerts = currentState.activeAlerts.map { alert ->
                if (alert.id == alertId) {
                    alert.copy(isResolved = true)
                } else {
                    alert
                }
            }
            _state.value = currentState.copy(
                activeAlerts = updatedAlerts.filter { !it.isResolved },
                lastUpdated = LocalDateTime.now()
            )
        }
    }

    /**
     * Port of: get_active_alerts()
     * Returns all unresolved alerts
     */
    fun getActiveAlerts(): List<OperationalAlert> {
        return _state.value.activeAlerts.filter { !it.isResolved }
    }

    // ==================== METRICS & REPORTING ====================

    /**
     * Port of: compute_daily_metrics()
     * Computes daily operational metrics
     */
    fun computeDailyMetrics() {
        viewModelScope.launch {
            val currentState = _state.value
            val metrics = DailyMetrics(
                date = LocalDateTime.now().toLocalDate().toString(),
                totalOrders = currentState.completedOrders.size,
                totalRevenue = currentState.totalRevenue,
                averageOrderValue = if (currentState.completedOrders.isEmpty()) 0.0 
                    else currentState.totalRevenue / currentState.completedOrders.size,
                averagePrepTime = getAveragePrepTime(),
                customerSatisfactionScore = 5.0,
                staffEfficiencyScore = calculateStaffEfficiency(),
                foodCostPercentage = getFoodCostPercentage(),
                laborCostPercentage = getLaborCostPercentage(),
                profitMargin = if (currentState.totalRevenue == 0.0) 0.0 
                    else (calculateProfit() / currentState.totalRevenue) * 100
            )
            _state.value = currentState.copy(
                dailyMetrics = metrics,
                lastUpdated = LocalDateTime.now()
            )
        }
    }

    /**
     * Port of: calculate_staff_efficiency()
     * Calculates overall staff efficiency score
     */
    private fun calculateStaffEfficiency(): Double {
        val state = _state.value
        if (state.activeStaff.isEmpty()) return 100.0
        return state.activeStaff.map { it.performance.qualityScore }.average()
    }

    /**
     * Port of: get_daily_summary()
     * Returns a summary of the day's operations
     */
    fun getDailySummary(): String {
        val metrics = _state.value.dailyMetrics
        return """Daily Summary:
            Total Orders: ${metrics.totalOrders}
            Revenue: ${'$'}${String.format("%.2f", metrics.totalRevenue)}
            Profit: ${'$'}${String.format("%.2f", calculateProfit())}
            Labor Cost %: ${String.format("%.1f", metrics.laborCostPercentage)}%
            Food Cost %: ${String.format("%.1f", metrics.foodCostPercentage)}%
            Profit Margin: ${String.format("%.1f", metrics.profitMargin)}%
        """.trimIndent()
    }
}
