package com.example

import android.content.Context
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.InventoryDatabase
import com.example.data.InventoryRepository
import com.example.ui.InventoryAppScreen
import com.example.ui.InventoryViewModel
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("UPVC Stock", appName)
    }

    @Test
    fun `test app rendering and initial flow`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val database = Room.inMemoryDatabaseBuilder(context, InventoryDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val repository = InventoryRepository(database.inventoryDao)
        val viewModel = InventoryViewModel(repository)

        composeTestRule.setContent {
            InventoryAppScreen(viewModel = viewModel)
        }
        composeTestRule.waitForIdle()
        database.close()
    }
}

