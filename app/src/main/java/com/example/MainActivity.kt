package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.data.InventoryDatabase
import com.example.data.InventoryRepository
import com.example.ui.InventoryAppScreen
import com.example.ui.InventoryViewModel
import com.example.ui.InventoryViewModelFactory
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    private val database by lazy { InventoryDatabase.getDatabase(this) }
    private val repository by lazy { InventoryRepository(database.inventoryDao) }
    private val viewModel: InventoryViewModel by viewModels {
        InventoryViewModelFactory(repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Load customized theme settings on startup
        com.example.ui.theme.loadPersistedTheme(this)
        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    InventoryAppScreen(viewModel = viewModel)
                }
            }
        }
    }
}

