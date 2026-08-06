package com.greenleaf.paygo

import android.app.Application
import com.greenleaf.paygo.di.ServiceLocator
import kotlinx.coroutines.launch

class PaygoApp : Application() {
    override fun onCreate() {
        super.onCreate()
        ServiceLocator.init(this)
        // Seed built-in provider parsing/response rules on first launch.
        ServiceLocator.applicationScope.launch {
            ServiceLocator.repository.seedIfEmpty()
        }
    }
}
