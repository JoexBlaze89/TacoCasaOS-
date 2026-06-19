package com.tacocasa.os

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tacocasa.os.ui.theme.TacoCasaOSTheme
import com.tacocasa.os.viewmodel.TacoCasaViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TacoCasaOSTheme {
                Surface {
                    MainScreen()
                }
            }
        }
    }
}

@Composable
fun MainScreen(vm: TacoCasaViewModel = viewModel()) {
    Scaffold(topBar = { SmallTopAppBar(title = { Text("Taco Casa OS") }) }) { inner ->
        Text(
            "Welcome to TacoCasaOS — Compose port",
            modifier = Modifier.padding(inner).padding(16.dp)
        )
    }
}
