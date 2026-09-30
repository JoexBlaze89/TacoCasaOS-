package com.tacocasa.os.viewmodel

import com.tacocasa.os.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDateTime
import kotlin.math.abs
import kotlin.uuid.Uuid

/**
 * ViewModel for TacoCasaOS. Implements the business logic ported from taco_casa_os.py.
 * Each public method corresponds to a user action/screen in the app.
 * This is a shared, platform-independent viewmodel used across Android, iOS, Desktop, and Web.
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
class TacoCasaViewModel {

    private val _state = MutableStateFlow(TacoCasaState())
    val state: StateFlow<TacoCasaState> = _state.asStateFlow()

    private suspend fun saveState() {
        // Platform-specific persistence is handled by platform implementations
        saveToPersistence(_state.value)
    }

    /**
     * Platform-specific persistence - implemented via expect/actual
     */
    expect suspend fun saveToPersistence(state: TacoCasaState)
    expect suspend fun loadFromPersistence(): TacoCasaState

    // ==================== SHIFT MANAGEMENT ====================

    fun startShift(shift: Shift) {
        val currentState = _state.value
        val now = Clock.System.now().toString()
        _state.value = currentState.copy(
            isOpen = true,
            currentShift = shift,
            shiftStartTime = now,
            lastUpdated = now
        )
    }

    fun endShift() {
        val currentState = _state.value
        val now = Clock.System.now().toString()
        _state.value = currentState.copy(
            isOpen = false,
            currentShift = null,
            lastUpdated = now
        )
    }

    // ==================== RUSH PREDICTION ====================

    fun predictRush(): RushPrediction {
        val hour = Clock.System.now().toString().substring(11, 13).toIntOrNull() ?: 0
        return when (hour) {
            in 11..13 -> RushPrediction.LUNCH_APPROACHING
            in 17..19 -> RushPrediction.DINNER_APPROACHING
            else -> RushPrediction.NONE
        }
    }

    fun updateRushLevel(ordersInQueue: Int) {
        val rushLevel = when {
            ordersInQueue == 0 -> RushLevel.SLOW
            ordersInQueue in 1..3 -> RushLevel.NORMAL
            ordersInQueue in 4..7 -> RushLevel.BUSY
            ordersInQueue in 8..15 -> RushLevel.RUSH
            else -> RushLevel.EXTREME_RUSH
        }

        val currentState = _state.value
        val prediction = predictRush()
        val now = Clock.System.now().toString()
        _state.value = currentState.copy(
            rushLevel = rushLevel,
            rushPrediction = prediction,
            lastUpdated = now
        )
    }

    // ==================== FINANCIAL CALCULATIONS ====================

    fun calculateProfit(): Double {
        val state = _state.value
        return state.totalRevenue - (state.laborCosts + state.foodCosts + state.otherExpenses)
    }

    fun getFoodCostPercentage(): Double {
        val state = _state.value
        if (state.totalRevenue == 0.0) return 0.0
        return (state.foodCosts / state.totalRevenue) * 100
    }

    fun getLaborCostPercentage(): Double {
        val state = _state.value
        if (state.totalRevenue == 0.0) return 0.0
        return (state.laborCosts / state.totalRevenue) * 100
    }

    fun calculateLaborCosts(): Double {
        val state = _state.value
        return state.activeStaff.sumOf { staff ->
            staff.hoursWorked * staff.hourlyRate
        }
    }

    fun addRevenue(amount: Double) {
        val currentState = _state.value
        val newRevenue = currentState.totalRevenue + amount
        val now = Clock.System.now().toString()
        _state.value = currentState.copy(
            totalRevenue = newRevenue,
            profit = calculateProfit(),
            lastUpdated = now
        )
    }

    fun addExpense(amount: Double, category: String) {
        val currentState = _state.value
        val now = Clock.System.now().toString()
        val updatedState = when (category) {
            "LABOR" -> currentState.copy(laborCosts = currentState.laborCosts + amount)
            "FOOD" -> currentState.copy(foodCosts = currentState.foodCosts + amount)
            "OTHER" -> currentState.copy(otherExpenses = currentState.otherExpenses + amount)
            else -> currentState
        }
        _state.value = updatedState.copy(
            profit = calculateProfit(),
            lastUpdated = now
        )
    }

    // ==================== STAFF MANAGEMENT ====================

    fun clockInStaff(name: String, role: StaffRole, hourlyRate: Double) {
        val currentState = _state.value
        val now = Clock.System.now().toString()
        val staff = StaffMember(
            id = Uuid.random().toString(),
            name = name,
            role = role,
            hourlyRate = hourlyRate,
            isActive = true,
            startTime = now
        )
        _state.value = currentState.copy(
            activeStaff = currentState.activeStaff + staff,
            lastUpdated = now
        )
    }

    fun clockOutStaff(staffId: String) {
        val currentState = _state.value
        val now = Clock.System.now().toString()
        val updatedStaff = currentState.activeStaff.map { staff ->
            if (staff.id == staffId && staff.startTime != null) {
                staff.copy(isActive = false)
            } else {
                staff
            }
        }
        _state.value = currentState.copy(
            activeStaff = updatedStaff.filter { it.isActive },
            lastUpdated = now
        )
    }

    fun getActiveStaffCount(): Int {
        return _state.value.activeStaff.count { it.isActive }
    }

    // ==================== INVENTORY MANAGEMENT ====================

    fun addInventoryItem(
        name: String,
        category: InventoryCategory,
        quantity: Double,
        unit: String,
        minimumThreshold: Double,
        maximumCapacity: Double,
        unitCost: Double
    ) {
        val currentState = _state.value
        val now = Clock.System.now().toString()
        val item = InventoryItem(
            id = Uuid.random().toString(),
            name = name,
            category = category,
            currentQuantity = quantity,
            unit = unit,
            minimumThreshold = minimumThreshold,
            maximumCapacity = maximumCapacity,
            unitCost = unitCost,
            lastRestockDate = now
        )
        val updatedInventory = currentState.inventory.toMutableMap()
        updatedInventory[item.id] = item
        _state.value = currentState.copy(
            inventory = updatedInventory,
            lastUpdated = now
        )
        checkInventoryThresholds()
    }

    fun updateInventory(itemId: String, quantityUsed: Double) {
        val currentState = _state.value
        val now = Clock.System.now().toString()
        val updatedInventory = currentState.inventory.toMutableMap()
        updatedInventory[itemId]?.let { item ->
            updatedInventory[itemId] = item.copy(
                currentQuantity = item.currentQuantity - quantityUsed
            )
        }
        _state.value = currentState.copy(
            inventory = updatedInventory,
            lastUpdated = now
        )
        checkInventoryThresholds()
    }

    fun restockItem(itemId: String, quantity: Double) {
        val currentState = _state.value
        val now = Clock.System.now().toString()
        val updatedInventory = currentState.inventory.toMutableMap()
        updatedInventory[itemId]?.let { item ->
            updatedInventory[itemId] = item.copy(
                currentQuantity = item.currentQuantity + quantity,
                lastRestockDate = now
            )
        }
        _state.value = currentState.copy(
            inventory = updatedInventory,
            lastUpdated = now
        )
        checkInventoryThresholds()
    }

    fun checkInventoryThresholds() {
        val currentState = _state.value
        val alerts = mutableListOf<InventoryAlert>()
        val now = Clock.System.now().toString()

        currentState.inventory.forEach { (_, item) ->
            when {
                item.currentQuantity <= 0 -> alerts.add(
                    InventoryAlert(
                        id = Uuid.random().toString(),
                        itemId = item.id,
                        itemName = item.name,
                        alertType = AlertType.OUT_OF_STOCK,
                        currentQuantity = item.currentQuantity,
                        threshold = item.minimumThreshold,
                        timestamp = now
                    )
                )
                item.currentQuantity < item.minimumThreshold -> alerts.add(
                    InventoryAlert(
                        id = Uuid.random().toString(),
                        itemId = item.id,
                        itemName = item.name,
                        alertType = AlertType.LOW_STOCK,
                        currentQuantity = item.currentQuantity,
                        threshold = item.minimumThreshold,
                        timestamp = now
                    )
                )
            }
        }
        _state.value = currentState.copy(
            inventoryAlerts = alerts,
            lastUpdated = now
        )
    }

    fun getLowStockItems(): List<InventoryItem> {
        return _state.value.inventory.values.filter { item ->
            item.currentQuantity < item.minimumThreshold
        }
    }

    // ==================== KITCHEN OPERATIONS ====================

    fun createOrder(items: List<OrderItem>, priority: Int = 0) {
        val currentState = _state.value
        val now = Clock.System.now().toString()
        val totalCost = items.sumOf { it.quantity * it.pricePerUnit }
        val order = KitchenOrder(
            id = Uuid.random().toString(),
            orderNumber = currentState.ordersInProgress.size + currentState.completedOrders.size + 1,
            items = items,
            status = OrderStatus.PENDING,
            createdTime = now,
            estimatedTime = calculateEstimatedTime(items),
            priority = priority,
            totalCost = totalCost
        )
        _state.value = currentState.copy(
            ordersInProgress = currentState.ordersInProgress + order,
            lastUpdated = now
        )
        updateRushLevel(currentState.ordersInProgress.size + 1)
    }

    fun startCooking(orderId: String) {
        val currentState = _state.value
        val now = Clock.System.now().toString()
        val updatedOrders = currentState.ordersInProgress.map { order ->
            if (order.id == orderId) {
                order.copy(
                    status = OrderStatus.IN_PROGRESS,
                    startCookTime = now
                )
            } else {
                order
            }
        }
        _state.value = currentState.copy(
            ordersInProgress = updatedOrders,
            lastUpdated = now
        )
    }

    fun completeOrder(orderId: String) {
        val currentState = _state.value
        val now = Clock.System.now().toString()
        val completedOrder = currentState.ordersInProgress.find { it.id == orderId }?.copy(
            status = OrderStatus.READY,
            completedTime = now
        )

        if (completedOrder != null) {
            _state.value = currentState.copy(
                ordersInProgress = currentState.ordersInProgress.filter { it.id != orderId },
                completedOrders = currentState.completedOrders + completedOrder,
                lastUpdated = now
            )
        }
    }

    private fun calculateEstimatedTime(items: List<OrderItem>): Int {
        return 5 + (items.sumOf { it.quantity } * 2)
    }

    fun getOrdersByPriority(): List<KitchenOrder> {
        return _state.value.ordersInProgress.sortedByDescending { it.priority }
    }

    fun getAveragePrepTime(): Double {
        val completedOrders = _state.value.completedOrders.filter { it.actualTime != null }
        if (completedOrders.isEmpty()) return 0.0
        return completedOrders.map { it.actualTime ?: 0 }.average()
    }

    // ==================== METRICS & REPORTING ====================

    fun computeDailyMetrics() {
        val currentState = _state.value
        val now = Clock.System.now().toString()
        val metrics = DailyMetrics(
            date = now.substring(0, 10),
            totalOrders = currentState.completedOrders.size,
            totalRevenue = currentState.totalRevenue,
            averageOrderValue = if (currentState.completedOrders.isEmpty()) 0.0
            else currentState.totalRevenue / currentState.completedOrders.size,
            averagePrepTime = getAveragePrepTime(),
            foodCostPercentage = getFoodCostPercentage(),
            laborCostPercentage = getLaborCostPercentage(),
            profitMargin = if (currentState.totalRevenue == 0.0) 0.0
            else (calculateProfit() / currentState.totalRevenue) * 100
        )
        _state.value = currentState.copy(
            dailyMetrics = metrics,
            lastUpdated = now
        )
    }

    fun getDailySummary(): String {
        val metrics = _state.value.dailyMetrics
        return """Daily Summary:
            |Total Orders: ${metrics.totalOrders}
            |Revenue: \$${String.format("%.2f", metrics.totalRevenue)}
            |Profit: \$${String.format("%.2f", calculateProfit())}
            |Labor Cost %: ${String.format("%.1f", metrics.laborCostPercentage)}%
            |Food Cost %: ${String.format("%.1f", metrics.foodCostPercentage)}%
            |Profit Margin: ${String.format("%.1f", metrics.profitMargin)}%
        """.trimMargin()
    }
}
