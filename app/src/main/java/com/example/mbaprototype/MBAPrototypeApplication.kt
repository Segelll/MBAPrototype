package com.example.mbaprototype

import android.app.Application
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import com.example.mbaprototype.ui.SharedViewModel


class MBAPrototypeApplication : Application(), ViewModelStoreOwner {


    private val appViewModelStore: ViewModelStore by lazy {
        ViewModelStore()
    }

    override val viewModelStore: ViewModelStore
        get() = appViewModelStore


    val sharedViewModel: SharedViewModel by lazy {
        ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(this)
        )[SharedViewModel::class.java]
    }

}