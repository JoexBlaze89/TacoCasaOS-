package com.tacocasa.os

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tacocasa.os.ui.screens.CleaningScreen
import com.tacocasa.os.ui.screens.HomeScreen
import com.tacocasa.os.ui.screens.InventoryScreen
import com.tacocasa.os.ui.screens.KitchenScreen
import com.tacocasa.os.ui.screens.NotesScreen
import com.tacocasa.os.ui.screens.PrepScreen
import com.tacocasa.os.viewmodel.TacoCasaViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TacoCasaOSApp()
        }
    }
}

@Composable
fun TacoCasaOSApp() {
    val viewModel: TacoCasaViewModel = viewModel()
    var selectedTab by remember { mutableIntStateOf(0) }

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    icon = { Icon(Icons.Filled.Home, "Home") },
                    label = { Text("Home") },
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Filled.ShoppingCart, "Prep") },
                    label = { Text("Prep") },
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Filled.Settings, "Kitchen") },
                    label = { Text("Kitchen") },
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Filled.Info, "Inventory") },
                    label = { Text("Inventory") },
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Filled.Notifications, "Cleaning") },
                    label = { Text("Cleaning") },
                    selected = selectedTab == 4,
                    onClick = { selectedTab = 4 }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Filled.Notifications, "Notes") },
                    label = { Text("Notes") },
                    selected = selectedTab == 5,
                    onClick = { selectedTab = 5 }
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                0 -> HomeScreen(viewModel)
                1 -> PrepScreen(viewModel)
                2 -> KitchenScreen(viewModel)
                3 -> InventoryScreen(viewModel)
                4 -> CleaningScreen(viewModel)
                5 -> NotesScreen(viewModel)
            }
        }
    }
}
