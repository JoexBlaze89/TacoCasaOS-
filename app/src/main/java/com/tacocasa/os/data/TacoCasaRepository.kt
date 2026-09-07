package com.tacocasa.os.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.tacocasa.os.model.TacoCasaState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

val Context.tacoCasaDataStore: DataStore<Preferences> by preferencesDataStore(name = "taco_casa_state")

@Serializable
data class PersistedStateEnvelope(val schemaVersion: Int, val state: TacoCasaState)

class TacoCasaRepository(private val context: Context) {
    companion object {
        private val STATE_KEY = stringPreferencesKey("taco_casa_state_json")
        private val LAST_UPDATED_KEY = stringPreferencesKey("last_updated")
        private val EVENTS_KEY = stringPreferencesKey("taco_casa_event_ledger_json")
        private const val MAX_EVENTS = 2000
    }

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true; explicitNulls = true }
    private val writeMutex = Mutex()

    val stateFlow: Flow<TacoCasaState> = context.tacoCasaDataStore.data.map { preferences ->
        preferences[STATE_KEY]?.let(::deserializeState) ?: TacoCasaState()
    }

    val eventFlow: Flow<List<OperationalEvent>> = context.tacoCasaDataStore.data.map { preferences ->
        preferences[EVENTS_KEY]?.let(::deserializeEvents) ?: emptyList()
    }

    suspend fun saveState(state: TacoCasaState) {
        writeMutex.withLock {
            context.tacoCasaDataStore.edit { preferences ->
                preferences[STATE_KEY] = serializeState(state)
                preferences[LAST_UPDATED_KEY] = state.lastUpdated.toString()
            }
        }
    }

    suspend fun loadState(): TacoCasaState = stateFlow.first()

    suspend fun appendEvent(event: OperationalEvent) {
        writeMutex.withLock {
            context.tacoCasaDataStore.edit { preferences ->
                val events = preferences[EVENTS_KEY]?.let(::deserializeEvents).orEmpty()
                preferences[EVENTS_KEY] = json.encodeToString((events + event).takeLast(MAX_EVENTS))
            }
        }
    }

    suspend fun loadEvents(): List<OperationalEvent> = eventFlow.first()

    suspend fun clearState() {
        writeMutex.withLock { context.tacoCasaDataStore.edit { it.clear() } }
    }

    private fun serializeState(state: TacoCasaState): String =
        json.encodeToString(PersistedStateEnvelope(StateSchema.CURRENT_VERSION, state))

    private fun deserializeState(rawJson: String): TacoCasaState {
        return try {
            val envelope = json.decodeFromString<PersistedStateEnvelope>(rawJson)
            migrateState(envelope.schemaVersion, envelope.state)
        } catch (_: SerializationException) {
            runCatching { json.decodeFromString<TacoCasaState>(rawJson) }.getOrElse { TacoCasaState() }
        } catch (_: IllegalArgumentException) {
            TacoCasaState()
        }
    }

    private fun migrateState(version: Int, state: TacoCasaState): TacoCasaState = when {
        version == StateSchema.CURRENT_VERSION -> state
        version < StateSchema.CURRENT_VERSION -> state
        else -> TacoCasaState()
    }

    private fun deserializeEvents(rawJson: String): List<OperationalEvent> =
        runCatching { json.decodeFromString<List<OperationalEvent>>(rawJson) }.getOrDefault(emptyList())
}
