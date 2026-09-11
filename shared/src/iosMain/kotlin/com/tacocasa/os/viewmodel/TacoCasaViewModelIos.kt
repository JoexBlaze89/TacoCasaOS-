package com.tacocasa.os.viewmodel

import com.tacocasa.os.model.TacoCasaState
import kotlinx.serialization.json.Json

/**
 * iOS-specific persistence layer using UserDefaults via Kotlin/Native interop
 */
actual suspend fun TacoCasaViewModel.saveToPersistence(state: TacoCasaState) {
    val json = Json.encodeToString(TacoCasaState.serializer(), state)
    // Save to UserDefaults or Keychain
}

actual suspend fun TacoCasaViewModel.loadFromPersistence(): TacoCasaState {
    // Load from UserDefaults or Keychain
    return TacoCasaState()
}
