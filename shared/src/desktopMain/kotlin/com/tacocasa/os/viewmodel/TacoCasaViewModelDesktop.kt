package com.tacocasa.os.viewmodel

import com.tacocasa.os.model.TacoCasaState
import kotlinx.serialization.json.Json
import java.io.File

/**
 * Desktop (JVM) persistence layer using JSON files
 */
actual suspend fun TacoCasaViewModel.saveToPersistence(state: TacoCasaState) {
    val json = Json.encodeToString(TacoCasaState.serializer(), state)
    val file = File(System.getProperty("user.home"), ".tacocasa/state.json")
    file.parentFile?.mkdirs()
    file.writeText(json)
}

actual suspend fun TacoCasaViewModel.loadFromPersistence(): TacoCasaState {
    val file = File(System.getProperty("user.home"), ".tacocasa/state.json")
    return if (file.exists()) {
        Json.decodeFromString(TacoCasaState.serializer(), file.readText())
    } else {
        TacoCasaState()
    }
}
