package com.greenleaf.paygo.ui.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.greenleaf.paygo.di.ServiceLocator
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn

class TransactionsViewModel : ViewModel() {
    private val repo = ServiceLocator.repository
    val transactions = repo.observeTransactions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}

class GroupsViewModel : ViewModel() {
    private val repo = ServiceLocator.repository
    val groups = repo.observeGroups()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun messagesFor(groupKey: String) = repo.observeMessagesByGroup(groupKey)
}
