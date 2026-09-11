package com.tacocasa.os.viewmodel

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.tacocasa.os.model.TacoCasaState
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "taco_casa_state")

actual suspend fun TacoCasaViewModel.saveToPersistence(state: TacoCasaState) {
    // For Android, save to DataStore when context is available
    // This is a placeholder; actual implementation requires context injection
    val json = Json.encodeToString(TacoCasaState.serializer(), state)
    // Save to DataStore or SharedPreferences
}

actual suspend fun TacoCasaViewModel.loadFromPersistence(): TacoCasaState {
    // For Android, load from DataStore when context is available
    return TacoCasaState()
}
