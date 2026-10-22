package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.room.Room
import com.example.data.OutageApiService
import com.example.data.OutageDatabase
import com.example.data.OutageRepository
import com.example.ui.OutageDashboardScreen
import com.example.ui.OutageViewModel
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val database = Room.databaseBuilder(
            applicationContext,
            OutageDatabase::class.java,
            "bicol_nap_outage_db"
        ).fallbackToDestructiveMigration(dropAllTables = true).build()

        val apiService = OutageApiService.create()
        val repository = OutageRepository(
            context = applicationContext,
            apiService = apiService,
            snapshotDao = database.snapshotDao()
        )

        setContent {
            MyApplicationTheme {
                val factory = remember { OutageViewModel.provideFactory(repository) }
                val outageViewModel: OutageViewModel = viewModel(factory = factory)
                OutageDashboardScreen(viewModel = outageViewModel)
            }
        }
    }
}
