package com.tacocasa.os.data

import kotlinx.serialization.Serializable
import java.time.LocalDateTime
import java.util.UUID

/** Immutable historical record used by the Observe -> ... -> Learn loop. */
@Serializable
data class OperationalEvent(
    val id: String = UUID.randomUUID().toString(),
    val timestamp: String = LocalDateTime.now().toString(),
    val type: EventType,
    val summary: String,
    val stateSchemaVersion: Int = StateSchema.CURRENT_VERSION,
    val source: String = "TacoCasaOS"
)

@Serializable
enum class EventType {
    STATE_CHANGED,
    SHIFT_STARTED,
    SHIFT_ENDED,
    RUSH_CHANGED,
    REVENUE_CHANGED,
    EXPENSE_CHANGED,
    STAFF_CHANGED,
    INVENTORY_CHANGED,
    ORDER_CREATED,
    ORDER_STARTED,
    ORDER_COMPLETED,
    PREP_CHANGED,
    CLEANING_CHANGED,
    MAINTENANCE_CHANGED,
    NOTE_ADDED,
    COMPLAINT_CHANGED,
    RECOVERY_CHANGED,
    ALERT_CHANGED,
    METRICS_CHANGED
}

object StateSchema {
    const val CURRENT_VERSION = 1
}
