package com.tacocasa.os

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.tacocasa.os.data.TacoCasaRepository
import com.tacocasa.os.model.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDateTime

@RunWith(AndroidJUnit4::class)
class TacoCasaRepositoryPersistenceTest {
    private lateinit var repository: TacoCasaRepository

    @Before
    fun setUp() = runBlocking {
        repository = TacoCasaRepository(ApplicationProvider.getApplicationContext())
        repository.clearState()
    }

    @Test
    fun write_kill_recreate_read_compare() = runBlocking {
        val now = LocalDateTime.now().withNano(0)
        val state = TacoCasaState(
            isOpen = true,
            currentShift = Shift.EVENING,
            rushLevel = RushLevel.BUSY,
            totalRevenue = 247.50,
            foodCosts = 61.25,
            activeStaff = listOf(
                StaffMember("staff-1", "Test Cook", StaffRole.COOK, 16.0, startTime = now)
            ),
            inventory = mapOf(
                "item-1" to InventoryItem("item-1", "Tortillas", InventoryCategory.GRAINS, 42.0, "pieces", 20.0, 100.0, 0.10)
            ),
            ordersInProgress = listOf(
                KitchenOrder("order-1", 1, listOf(OrderItem("Taco", 2, pricePerUnit = 2.50)), OrderStatus.IN_PROGRESS, now, estimatedTime = 9)
            ),
            shiftStartTime = now,
            lastUpdated = now
        )

        repository.saveState(state)

        // Simulate process death/recreation by constructing a completely new repository instance.
        val recreatedRepository = TacoCasaRepository(ApplicationProvider.getApplicationContext())
        val restored = recreatedRepository.loadState()

        assertEquals(state, restored)
    }

    @Test
    fun event_ledger_survives_recreation() = runBlocking {
        repository.appendEvent(
            com.tacocasa.os.data.OperationalEvent(
                type = com.tacocasa.os.data.EventType.SHIFT_STARTED,
                summary = "Shift started: EVENING"
            )
        )

        val recreatedRepository = TacoCasaRepository(ApplicationProvider.getApplicationContext())
        val events = recreatedRepository.loadEvents()

        assertEquals(1, events.size)
        assertEquals(com.tacocasa.os.data.EventType.SHIFT_STARTED, events.single().type)
    }
}
