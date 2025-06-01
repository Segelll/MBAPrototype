package com.example.mbaprototype

import android.app.Application
import com.example.mbaprototype.data.DataSource
import com.example.mbaprototype.ui.SharedViewModel
import com.example.mbaprototype.utils.InteractionLogger // Import InteractionLogger

class MBAPrototypeApplication : Application() {

    val sharedViewModel: SharedViewModel by lazy {
        SharedViewModel(this) // Pass application context to ViewModel
    }

    override fun onCreate() {
        super.onCreate()
        DataSource.init(this)
        InteractionLogger.initialize(this) // Initialize the logger
    }
}