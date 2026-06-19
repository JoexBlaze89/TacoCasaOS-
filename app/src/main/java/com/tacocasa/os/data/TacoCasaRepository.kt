package com.tacocasa.os.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.tacocasa.os.model.TacoCasaState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

val Context.tacoCasaDataStore: DataStore<Preferences> by preferencesDataStore(name = "taco_casa_state")

class TacoCasaRepository(private val context: Context) {
    companion object {
        private val STATE_KEY = stringPreferencesKey("taco_casa_state_json")
        private val LAST_UPDATED_KEY = stringPreferencesKey("last_updated")
    }

    /**
     * Observe the current state as a Flow
     * Returns the saved state or a default empty state if nothing is saved
     */
    val stateFlow: Flow<TacoCasaState> = context.tacoCasaDataStore.data.map { preferences ->
        val stateJson = preferences[STATE_KEY] ?: return@map TacoCasaState()
        try {
            // In a real app, use a JSON serializer (e.g., Kotlinx Serialization or Gson)
            // For now, this is a placeholder that returns default state
            TacoCasaState()
        } catch (e: Exception) {
            e.printStackTrace()
            TacoCasaState()
        }
    }

    /**
     * Save the entire state to DataStore
     */
    suspend fun saveState(state: TacoCasaState) {
        context.tacoCasaDataStore.edit { preferences ->
            try {
                // In a real app, serialize to JSON using your preferred serializer
                val stateJson = serializeState(state)
                preferences[STATE_KEY] = stateJson
                preferences[LAST_UPDATED_KEY] = LocalDateTime.now().format(
                    DateTimeFormatter.ISO_DATE_TIME
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Load the state from storage
     */
    suspend fun loadState(): TacoCasaState {
        return try {
            context.tacoCasaDataStore.data.map { preferences ->
                val stateJson = preferences[STATE_KEY] ?: return@map TacoCasaState()
                deserializeState(stateJson)
            }.collect { state -> return state }
            TacoCasaState()
        } catch (e: Exception) {
            e.printStackTrace()
            TacoCasaState()
        }
    }

    /**
     * Clear all saved state
     */
    suspend fun clearState() {
        context.tacoCasaDataStore.edit { preferences ->
            preferences.clear()
        }
    }

    /**
     * Placeholder for JSON serialization
     * Replace with actual implementation using Kotlinx Serialization or Gson
     */
    private fun serializeState(state: TacoCasaState): String {
        // TODO: Implement JSON serialization
        return "{}"
    }

    /**
     * Placeholder for JSON deserialization
     * Replace with actual implementation using Kotlinx Serialization or Gson
     */
    private fun deserializeState(json: String): TacoCasaState {
        // TODO: Implement JSON deserialization
        return TacoCasaState()
    }
}
