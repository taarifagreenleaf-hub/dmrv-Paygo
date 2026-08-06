package com.greenleaf.paygo.ui.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.greenleaf.paygo.data.prefs.PaygoSettings
import com.greenleaf.paygo.di.ServiceLocator
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DashboardState(
    val messageCount: Int = 0,
    val transactionCount: Int = 0,
    val totalReceived: Double = 0.0
)

class DashboardViewModel : ViewModel() {
    private val repo = ServiceLocator.repository
    private val settingsStore = ServiceLocator.settingsStore

    val settings = settingsStore.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PaygoSettings())

    val messageCount = repo.observeMessageCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
    val transactionCount = repo.observeTransactionCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
    val totalReceived = repo.observeTotalReceived()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val recentEvents = repo.observeEvents()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setMasterEnabled(enabled: Boolean) = viewModelScope.launch {
        settingsStore.update { it.copy(masterEnabled = enabled) }
    }

    fun setAutoReply(enabled: Boolean) = viewModelScope.launch {
        settingsStore.update { it.copy(autoReplyEnabled = enabled) }
    }
}
